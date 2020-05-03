package matsim.parser

import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import matsim.model.*
import java.lang.Integer.max
import kotlin.contracts.ExperimentalContracts


interface Parser<T> {
    fun parse(path: String): T
}

const val simulationNodeLength = 7

@ExperimentalContracts
class OsmParser : Parser<List<Way>> {
    private val connector = Connector()
    private val prohibitedHighways =
        setOf(
            "street_lamp",
            "footway",
            "path",
            "bus_stop",
            "construction",
            "proposed",
            "pedestrian",
            "living_street",
            "proposed",
            "track",
            "cycleway",
            "service"
        )

    override fun parse(path: String): List<Way> {
        val json = parseFile(path)!!
        val elements = json["elements"] as JsonArray<JsonObject>

        val osmNodeMap = elements.filter { it["type"] == "node" }
            .filterProhibitedHighways()
            .filter { it.tag("man_made") != "bridge" }
            .map { parseToNode(it) }
            .associateBy { it.id }
        val ways = elements.filter { it["type"] == "way" }
            .filterProhibitedHighways()
            .filter { it.tag("man_made") != "bridge" || it.tag("area:highway") == null }
            .map { parseToWay(it, osmNodeMap) }
        return ways.map { createSimulationNodes(it) }.also {
            connector.connect()
            println("Done")
        }
    }

    private fun List<JsonObject>.filterProhibitedHighways() =
        filter { prohibitedHighways.contains(it.tag("highway") ?: "").not() }

    private fun parseFile(name: String): JsonObject? {
        val cls = com.beust.klaxon.Parser::class.java
        return cls.getResourceAsStream(name)?.let { inputStream ->
            return com.beust.klaxon.Parser.default().parse(inputStream) as JsonObject
        }
    }

    private fun createSimulationNodes(way: OsmWay): Way {
        val lanes = if (!way.oneWay) max(way.lanes / 2, 1) else way.lanes
//        val lanes = 1
        val oneWay = way.nodes
            .createLinkBetweenNodes(way, lanes)
        val twoWay = if (!way.oneWay) way.nodes.reversed().createLinkBetweenNodes(way, lanes) else emptyList()
        return Way(way.id, oneWay, twoWay)
    }

    private fun List<OsmNode>.createLinkBetweenNodes(way: OsmWay, lanes: Int) = this.zipWithNext()
        .flatMap { zipped ->
            createLinkBetweenNodes(zipped, way.maxSpeed, way.id, lanes)
        }.merge(lanes)

    private fun List<Lane>.merge(lanes: Int): List<Lane> {
        val chunked = this.chunked(lanes)
        return chunked.mapIndexed { index, l ->
            if (index > 0) {
                merge(chunked[index - 1], chunked[index])
            } else l.flatten()
        }
    }

    private fun merge(prev: List<Lane>, current: List<Lane>): List<Node> =
        (prev.indices).map { prev[it] to current[it] }
            .flatMap { pair ->
                val last = pair.first.last()
                val next = pair.second.first()
                last.neighborhood.putAll(next.neighborhood)
                next.neighborhood.forEach {
                    it.value.neighborhood[it.key.opposite()] = last
                }
                if (pair.second.size > 1) {
                    pair.second.drop(1)
                } else pair.second
            }.distinct()


    private fun createLinkBetweenNodes(
        zipped: Pair<OsmNode, OsmNode>,
        maxSpeed: Speed,
        wayId: String,
        lanes: Int
    ): List<Lane> {
        val nodeLanes = (0 until lanes)
            .map { zipped.createNodesBetween(maxSpeed, wayId) }
        if (lanes > 1) {
            (0 until lanes).zipWithNext().forEach { lanePair ->
                val first = nodeLanes[lanePair.first]
                val second = nodeLanes[lanePair.second]
                connectLanes(first, second)
            }
        }
        return nodeLanes
    }


    private fun connectLanes(first: List<Node>, second: List<Node>) = first.zip(second).forEach { nodes ->
        nodes.first.neighborhood[Direction.TOP] = nodes.second
        nodes.second.neighborhood[Direction.BOTTOM] = nodes.first

    }


    private fun Pair<OsmNode, OsmNode>.createNodesBetween(maxSpeed: Speed, wayId: String): List<Node> {
        val distance = first.computeDistance(second)
        val numberOfNodes = max((distance / simulationNodeLength).toInt(), 1)
        val (deltaLat, deltaLon) = (second - first) / numberOfNodes
        val res = (0..numberOfNodes).asSequence()
            .map {
                createNodesBetween(deltaLat, deltaLon, maxSpeed, it, numberOfNodes, wayId)
            }
            .zipWithNext()
            .onEach {
                it.first.neighborhood[Direction.RIGHT] = it.second
                it.second.neighborhood[Direction.LEFT] = it.first
            }.fold(listOf<Node>()) { acc, pair -> acc.plus(pair.first).plus(pair.second) }.distinct()
        return res
    }

    private fun Pair<OsmNode, OsmNode>.createNodesBetween(
        deltaLat: Double,
        deltaLon: Double,
        maxSpeed: Speed,
        number: Int,
        totalNumber: Int,
        wayId: String
    ): Node {
        val newId = createId();
        return when (number) {
            0 -> {
                val node = first.copy(id = "${first.id}__$newId", maxSpeed = maxSpeed, wayId = wayId).toSimulationNode()
                connector.addConnection(first.id, node)
                node
            }
            totalNumber -> {
                val node =
                    second.copy(id = "${second.id}__$newId", maxSpeed = maxSpeed, wayId = wayId).toSimulationNode()
                connector.addConnection(second.id, node)
                node
            }
            else -> OsmNode(
                id = newId,
                maxSpeed = maxSpeed,
                lat = (first.lat + deltaLat * number).round(),
                long = (first.long + deltaLon * number).round(),
                isTrafficLight = false,
                isCrossRoad = false,
                wayId = wayId
            ).toSimulationNode()
        }
    }


    private fun parseToNode(json: JsonObject) = OsmNode(
        id = json.long("id")?.toString()!!,
        lat = json.double("lat")!!,
        long = json.double("lon")!!,
        isCrossRoad = json.tag("junction").isNotNull(),
        isTrafficLight = json.tag("highway") == "traffic_signals"
    )


    private fun JsonObject.tag(name: String) = this.obj("tags")?.string(name)
    private fun parseToWay(json: JsonObject, nodeMap: Map<String, OsmNode>) = OsmWay(
        id = json.long("id")!!.toString(),
        nodes = json.array<Long>("nodes")!!.map { it.toString() }.mapNotNull { nodeMap[it] },
        maxSpeed = json.tag("maxspeed")?.toDouble() ?: 70.0 / 10,
        lanes = json.tag("lanes")?.toInt() ?: 1,
        oneWay = json.tag("oneway") != null || json.tag("oneway") == "yes",
        secondary = json.tag("highway") == "secondary"
    )

    private fun OsmNode.toSimulationNode(): Node {
        if (isTrafficLight) {
            return TrafficLightNode(
                id = NodeId(id),
                x = lat,
                y = long,
                maxSpeed = maxSpeed,
                wayId = wayId
            )
        } else {
            return BasicNode(
                id = NodeId(id),
                x = lat,
                y = long,
                maxSpeed = maxSpeed,
                wayId = wayId
            )
        }
    }
}


