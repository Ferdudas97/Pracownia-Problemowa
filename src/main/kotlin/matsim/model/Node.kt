package matsim.model

import matsim.parser.earthRadius
import matsim.parser.toRadians
import java.time.Duration
import kotlin.contracts.ExperimentalContracts
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt


typealias Coordinate = Double


enum class Direction {
    TOP, LEFT, RIGHT, BOTTOM
}

sealed class Node {
    abstract val x: Coordinate
    abstract val y: Coordinate
    abstract val neighborhood: MutableMap<Direction, Node>
    abstract val maxSpeed: Speed
    abstract val id: NodeId;
    abstract val wayId: String

    fun computeDistance(other: Node): Double {
        // algorithm from https://www.movable-type.co.uk/scripts/latlong.html
        val latInRadians = x.toRadians()
        val longInRadians = y.toRadians()
        val otherLatInRadians = other.x.toRadians()
        val otherLongInRadians = other.y.toRadians()
        val latDelta = latInRadians - otherLatInRadians
        val longDelta = longInRadians - otherLongInRadians
        val a =
            sin(latDelta / 2) * sin(latDelta / 2) + cos(latInRadians) * cos(otherLatInRadians) * sin(longDelta / 2) * sin(
                longDelta / 2
            )
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c
    }

    fun iterate(i: Int): List<Node> {
        var node: Node
        val result = mutableListOf<Node>()
        result.add(this)
        return runCatching {
            while (result.size < i) {
                if (neighborhood[Direction.RIGHT] != null) {
                    node = neighborhood[Direction.RIGHT]!!
                    result.add(node)
                } else {
                    node =
                        neighborhood.filter { it.key != Direction.LEFT && it.value.neighborhood[Direction.RIGHT] != null }
                            .values.random()
                    result.add(node)
                }
            }
            result
        }.getOrDefault(result)
    }

    override fun equals(other: Any?): Boolean {

        return if (other is Node) {
            (id == other.id).and(x == other.x).and(y == other.y)
        } else {
            false
        }
    }

    override fun hashCode(): Int {
        return id.hashCode() + x.hashCode() + y.hashCode()
    }

    override fun toString(): String {
        return "Node(id=$id)"
    }
}

inline class NodeId(val id: String = createId())

data class BasicNode(
    override val id: NodeId,
    override val x: Coordinate,
    override val y: Coordinate,
    override val neighborhood: MutableMap<Direction, Node> = mutableMapOf(),
    override val maxSpeed: Speed, override val wayId: String
) : Node() {
    override fun equals(other: Any?): Boolean {
        return super.equals(other)
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }

    override fun toString(): String {
        return super.toString()
    }
}

data class OccupiedNode(
    val vehicle: Vehicle,
    override val id: NodeId,
    override val x: Coordinate,
    override val y: Coordinate,
    override val neighborhood: MutableMap<Direction, Node> = mutableMapOf(),
    override val maxSpeed: Speed, override val wayId: String
) : Node() {
    override fun equals(other: Any?): Boolean {
        return super.equals(other)
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }

    override fun toString(): String {
        return super.toString()
    }
}

enum class TrafficPhase {
    GREEN, RED
}


data class TrafficLightNode(
    val phase: TrafficPhase = TrafficPhase.GREEN,
    val phaseTime: Map<TrafficPhase, Duration> = mapOf(),
    override val x: Coordinate,
    override val y: Coordinate,
    override val neighborhood: MutableMap<Direction, Node> = mutableMapOf(),
    override val maxSpeed: Speed,
    override val id: NodeId, override val wayId: String
) : Node() {
    override fun equals(other: Any?): Boolean {
        return super.equals(other)
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }

    override fun toString(): String {
        return super.toString()
    }

}


data class ConnectorNode(
    override val x: Coordinate,
    override val y: Coordinate,
    override val neighborhood: MutableMap<Direction, Node>,
    override val maxSpeed: Speed,
    override val id: NodeId, override val wayId: String
) : Node() {

}

@ExperimentalContracts
class Connector() {
    private val connectionsMap = mutableMapOf<String, List<Node>>()
    fun addConnection(id: String, node: Node) {
        connectionsMap[id] = connectionsMap.getOrDefault(id, listOf()).plus(node)
    }

    fun connect() {
        connectionsMap.values.filter { it.size > 1 }.forEach(this::connect)

    }

    private fun connect(connections: List<Node>) = connections.forEach { node ->
        if (node.id.id.contains("1636298237")) {
            ""
        }
        val new = Direction.values().filter { !node.neighborhood.containsKey(it) }.map { dir ->
            dir to connections.shuffled().find {
                it != node && it.wayId != node.wayId
                        && !it.neighborhood.containsKey(dir.opposite())
            }
        }.associate { it }.filterValues { it != null }.mapValues { it.value!! }

        new.filter { it.value.neighborhood[it.key.opposite()] == null }
            .forEach {
                it.value.neighborhood[it.key.opposite()] = node
                node.neighborhood[it.key] = it.value
            }
    }

}


data class Way(
    val id: String,
    val oneWay: List<Lane> = emptyList(),
    val twoWay: List<Lane> = emptyList()
) {
    fun lanes() = oneWay + twoWay
}

fun Node.occupyBy(vehicle: Vehicle): OccupiedNode {
    return OccupiedNode(vehicle, id, x, y, neighborhood, maxSpeed, wayId)
}

fun OccupiedNode.release(): BasicNode {
    return BasicNode(id, x, y, neighborhood, maxSpeed, wayId)
}


fun Direction.getPossibleDirections() = when (this) {
    Direction.LEFT, Direction.RIGHT -> setOf(Direction.BOTTOM, Direction.TOP, this)
    Direction.TOP, Direction.BOTTOM -> setOf(Direction.RIGHT, Direction.LEFT, this)
}


fun Direction.opposite() = when (this) {
    Direction.TOP -> Direction.BOTTOM
    Direction.LEFT -> Direction.RIGHT
    Direction.BOTTOM -> Direction.TOP
    Direction.RIGHT -> Direction.LEFT
}