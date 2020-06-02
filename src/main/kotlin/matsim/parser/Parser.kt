package matsim.parser

import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import matsim.model.isNotNull
import kotlin.contracts.ExperimentalContracts


interface Parser<T> {
    fun parse(path: String): T
}

const val simulationNodeLength = 7

@ExperimentalContracts
class OsmParser : Parser<List<OsmWay>> {
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
            "steps",
            "service"
        )

    override fun parse(path: String): List<OsmWay> {
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
            .filter { it.tag("access") != "private" }
            .map { parseToWay(it, osmNodeMap) }
        return ways
    }

    private fun List<JsonObject>.filterProhibitedHighways() =
        filter { prohibitedHighways.contains(it.tag("highway") ?: "").not() }

    private fun parseFile(name: String): JsonObject? {
        val cls = com.beust.klaxon.Parser::class.java
        return cls.getResourceAsStream(name)?.let { inputStream ->
            return com.beust.klaxon.Parser.default().parse(inputStream) as JsonObject
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
        maxSpeed = (json.tag("maxspeed")?.toDouble() ?: 70.0) / 10,
        lanes = json.tag("lanes")?.toInt() ?: 1,
        oneWay = json.tag("oneway") != null && json.tag("oneway") == "yes",
        secondary = json.tag("highway") == "secondary"
    )
}


