package matsim.parser

import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import com.beust.klaxon.JsonReader
import com.beust.klaxon.Klaxon
import matsim.model.*
import matsim.simulation.Simulation
import org.w3c.dom.NamedNodeMap
import java.awt.Point
import java.io.File
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt


interface Parser<T> {
    fun parse(path: String): T
}

const val simulationNodeLength = 2

@ExperimentalContracts
class OsmParser : Parser<List<Node>> {
    private val idMappings = mutableMapOf<String, List<String>>()
    override fun parse(path: String): List<Node> {
        val json = parseFile(path)!!
        val elements = json["elements"] as JsonArray<JsonObject>
        val osmNodeMap = elements.filter { it["type"] == "node" }
            .map {
                parseToNode(it)
            }
            .associateBy { it.id }
        val ways = elements.filter { it["type"] == "way" }
            .map { parseToWay(it, osmNodeMap) }
        val r = ways.flatMap { createSimulationNodes(it) }
            .flatten()
            .map { it.toSimulationNode() }
        return r
    }

    private fun parseFile(name: String): JsonObject? {
        val cls = com.beust.klaxon.Parser::class.java
        return cls.getResourceAsStream(name)?.let { inputStream ->
            return com.beust.klaxon.Parser.default().parse(inputStream) as JsonObject
        }
    }

    private fun createSimulationNodes(way: Way) = way.nodes.zipWithNext()
        .flatMap { zipped ->
            val nodeLanes = (0 until way.lanes)
                .map { zipped.createNodesBetween(way.maxSpeed) }
            if (way.lanes > 1) {
                (0 until way.lanes).zipWithNext().onEach { lanePair ->
                    val first = nodeLanes[lanePair.first]
                    val second = nodeLanes[lanePair.second]
                    connectLanes(first, second)
                }
            }
            nodeLanes
        }


    private fun connectLanes(first: List<OsmNode>, second: List<OsmNode>) = first.zip(second).forEach { nodes ->
        nodes.first.neighbours[Direction.TOP] = nodes.second
        nodes.second.neighbours[Direction.BOTTOM] = nodes.first

    }


    private fun Pair<OsmNode, OsmNode>.createNodesBetween(maxSpeed: Speed): List<OsmNode> {
        val distance = first.computeDistance(second)
        val numberOfNodes = (distance / simulationNodeLength).toInt()
        val (deltaLat, deltaLon) = (first - second) / numberOfNodes
        return (0..numberOfNodes).asSequence()
            .map {
                createNodesBetween(deltaLat, deltaLon, maxSpeed, it, numberOfNodes)
            }
            .zipWithNext()
            .onEach {
                it.first.neighbours[Direction.RIGHT] = it.second
                it.second.neighbours[Direction.LEFT] = it.first
            }.fold(listOf<OsmNode>()) { acc, pair -> acc.plus(pair.first).plus(pair.second) }
            .toList()
    }

    private fun Pair<OsmNode, OsmNode>.createNodesBetween(
        deltaLat: Double,
        deltaLon: Double,
        maxSpeed: Speed,
        number: Int,
        totalNumber: Int
    ): OsmNode {
        val newId = createId();
        return when (number) {
            0 -> {
                addIdMapping(first.id, newId)
                first.copy(id = newId, maxSpeed = maxSpeed)
            }
            totalNumber -> {
                addIdMapping(second.id, newId)
                second.copy(newId, maxSpeed = maxSpeed)
            }
            else -> OsmNode(
                id = newId,
                maxSpeed = maxSpeed,
                lat = first.lat + deltaLat * number,
                long = first.long + deltaLon * number,
                isTrafficLight = false,
                isCrossRoad = false
            )
        }
    }


    private fun parseToNode(json: JsonObject) = OsmNode(
        id = json.int("id")?.toString()!!,
        lat = json.double("lat")!!,
        long = json.double("lon")!!,
        isCrossRoad = json.tag("junction").isNotNull(),
        isTrafficLight = json.tag("highway") == "traffic_signals"
    )


    private fun JsonObject.tag(name: String) = obj("tags")?.string(name)
    private fun parseToWay(json: JsonObject, nodeMap: Map<String, OsmNode>) = Way(
        id = json.int("id")!!.toString(),
        nodes = json.array<Int>("nodes")!!.map { it.toString() }.mapNotNull { nodeMap[it] },
        maxSpeed = json.tag("maxspeed")?.toDouble() ?: 70.0,
        lanes = json.tag("lanes")?.toInt() ?: 1
    )

    private fun addIdMapping(old: String, new: String) {
        idMappings[old] = idMappings[old].orEmpty().plus(new)
    }


    private fun OsmNode.toBasicNode(maxSpeed: Speed) = BasicNode(id = NodeId(), x = lat, y = long, maxSpeed = maxSpeed)
    private fun OsmNode.toSimulationNode(): Node {
        if (isTrafficLight) {
            return TrafficLightNode(id = NodeId(id), x = lat, y = long, maxSpeed = maxSpeed)
        } else {
            return BasicNode(id = NodeId(id), x = lat, y = long, maxSpeed = maxSpeed)
        }
    }
}


