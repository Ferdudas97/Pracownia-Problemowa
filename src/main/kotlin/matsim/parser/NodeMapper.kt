package matsim.parser

import matsim.model.*
import kotlin.contracts.ExperimentalContracts


@ExperimentalContracts
class NodeMapper() {
    private val connector = Connector()
    fun createSimulationWay(ways: List<OsmWay>): List<Way> {
        return ways.map { createSimulationNodes(it) }.also {
            connector.connect()
            println("Done")
        }
    }

    private fun createSimulationNodes(way: OsmWay): Way {
//        val lanes = if (!way.oneWay) Integer.max(way.lanes / 2, 1) else way.lanes
        val lanes = 1
        if (way.id == "127052510") {
            println("")
        }
        val oneWay = way.nodes
            .createLinkBetweenNodes(way, lanes)
        if (way.id == "127052510") {
            println("")
        }
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
                connector.removeConnection(next)
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
        val numberOfNodes = Integer.max((distance / simulationNodeLength).toInt(), 1)
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
                val node = first.copy(maxSpeed = maxSpeed, wayId = wayId).toSimulationNode("${first.id}__$newId")
                connector.addConnection(first.id, node)
                node
            }
            totalNumber -> {
                val node =
                    second.copy(maxSpeed = maxSpeed, wayId = wayId).toSimulationNode("${second.id}__$newId")
                connector.addConnection(second.id, node)
                node
            }
            else -> OsmNode(
                id = first.id,
                maxSpeed = maxSpeed,
                lat = (first.lat + deltaLat * number).round(),
                long = (first.long + deltaLon * number).round(),
                isTrafficLight = false,
                isCrossRoad = false,
                wayId = wayId
            ).toSimulationNode(newId)
        }
    }


    private fun OsmNode.toSimulationNode(newId: String): Node {
        if (isTrafficLight) {
            return TrafficLightNode(
                id = NodeId(newId),
                x = lat,
                y = long,
                maxSpeed = maxSpeed,
                wayId = wayId,
                osmId = id

            )
        } else {
            return BasicNode(
                id = NodeId(newId),
                x = lat,
                y = long,
                maxSpeed = maxSpeed,
                wayId = wayId,
                osmId = id
            )
        }
    }


}

fun main() {
    println(listOf(1, 2, 3, 4, 5, 6).zipWithNext())
}