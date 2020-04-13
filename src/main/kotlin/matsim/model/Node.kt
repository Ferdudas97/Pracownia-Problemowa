package matsim.model

import java.time.Duration
import kotlin.contracts.ExperimentalContracts


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

    fun addNeighbour(direction: Direction, node: Node) {
        neighborhood[direction] = node
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
    val phase: TrafficPhase = TrafficPhase.RED,
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
data class Connector(val nodeId: String = NodeId().id, val connections: List<Node> = listOf()) {
    fun addConnection(node: Node): Connector = copy(connections = connections.plus(node))
    fun connect() {
        connections.forEach { node ->
            val new = Direction.values().filter { !node.neighborhood.containsKey(it) }
                .associateWith { dir ->
                    connections.shuffled().find {
                        it != node && it.wayId != node.wayId
//                                && !it.neighborhood.containsKey()
                    }
                }
                .filterValues { it != null }.mapValues { it.value!! }
            node.neighborhood.putAll(new)
            new.filter { it.value.neighborhood[it.key.opposite()] == null }
                .forEach { it.value.neighborhood[it.key.opposite()] = node }
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