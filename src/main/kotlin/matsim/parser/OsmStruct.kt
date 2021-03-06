package matsim.parser

import matsim.model.Direction
import matsim.model.Speed
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

const val earthRadius: Long = 6371 * 1000 //earth radius in kilometers
fun Double.toRadians() = Math.toRadians(this)

data class OsmNode(
    val id: String,
    val lat: Double,
    val long: Double,
    val isCrossRoad: Boolean,
    val isTrafficLight: Boolean,
    val neighbours: MutableMap<Direction, OsmNode> = mutableMapOf(),
    val maxSpeed: Speed = 70.0,
    val wayId: String = "none"
) {
    fun computeDistance(other: OsmNode): Double {
        // algorithm from https://www.movable-type.co.uk/scripts/latlong.html
        val latInRadians = lat.toRadians()
        val longInRadians = long.toRadians()
        val otherLatInRadians = other.lat.toRadians()
        val otherLongInRadians = other.long.toRadians()
        val latDelta = latInRadians - otherLatInRadians
        val longDelta = longInRadians - otherLongInRadians
        val a =
            sin(latDelta / 2) * sin(latDelta / 2) + cos(latInRadians) * cos(otherLatInRadians) * sin(longDelta / 2) * sin(
                longDelta / 2
            )
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c
    }

    operator fun minus(other: OsmNode) = (lat - other.lat) to (long - other.long)

    operator fun div(o: Double) = lat / o to long / o
    override fun toString(): String {
        return id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return if (other is OsmNode) {
            return id == other.id
        } else false
    }
}

data class OsmWay(
    val id: String,
    val nodes: List<OsmNode>,
    val maxSpeed: Speed,
    val lanes: Int = 1,
    val oneWay: Boolean = false,
    val secondary: Boolean = false
) {
    fun length(): Double {
        return nodes.first().computeDistance(nodes.last())
    }
}
