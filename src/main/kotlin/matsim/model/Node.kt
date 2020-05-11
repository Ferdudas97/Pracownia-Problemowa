package matsim.model

import matsim.parser.earthRadius
import matsim.parser.toRadians
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

    fun getNeighboursInDirection(number: Int): List<Node> {
        var node: Node = this
        val result = mutableListOf<Node>()
        return runCatching {
            while (result.size < number) {
                if (node !is ConnectorNode && node !is TrafficLightNode) result.add(node)
                node = if (node.neighborhood[Direction.RIGHT] != null) {
                    node.neighborhood[Direction.RIGHT]!!

                } else {
                    node.neighborhood.filter { it.key != Direction.LEFT && it.value.neighborhood[Direction.RIGHT] != null }
                        .values.random()
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
    GREEN, RED;

    fun opposite(): TrafficPhase = when (this) {
        GREEN -> RED
        RED -> GREEN
    }
}


data class TrafficLightNode(
    val phase: TrafficPhase = TrafficPhase.GREEN,
    val phaseTime: Map<TrafficPhase, Int> = mapOf(),
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

    fun changePhase(step: Int): TrafficLightNode = phaseTime[phase]?.let {
        if (step % it == 0) {
            copy(phase = phase.opposite())
        } else this
    } ?: this

}


data class ConnectorNode(
    private val nodes: List<Node> = listOf(),
    override val x: Coordinate,
    override val y: Coordinate,
    override val maxSpeed: Speed,
    override val id: NodeId, override val wayId: String
) : Node() {
    override val neighborhood: MutableMap<Direction, Node>
        get() {
            val map = mutableMapOf<Direction, Node>()
            val chosenNodes = nodes.shuffled().take(3)
            chosenNodes.getOrNull(0)?.let { map[Direction.RIGHT] = it }
            chosenNodes.getOrNull(1)?.let { map[Direction.TOP] = it }
            chosenNodes.getOrNull(2)?.let { map[Direction.BOTTOM] = it }
            return map
        }

}

@ExperimentalContracts
class Connector {
    private val connectionsMap = mutableMapOf<String, List<Node>>()
    fun addConnection(id: String, node: Node) {
        connectionsMap[id] = connectionsMap.getOrDefault(id, listOf()).plus(node)
    }

    fun connect() {
        connectionsMap.values.filter { it.size > 1 }.forEach(this::connect)

    }

    private fun connect(connections: List<Node>) {
        if (connections.map(Node::wayId).distinct().size == 2) {
            connections.forEach { node ->
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
        } else {
            connections.forEach { node ->
                var new = Direction.values().filter { !node.neighborhood.containsKey(it) }.flatMap { dir ->
                    connections.shuffled().filter {
                        it != node && it.wayId != node.wayId && !it.neighborhood.containsKey(dir.opposite())
                    }
                }.distinct()
                if (new.isEmpty()) {
                    new = Direction.values().filter { !node.neighborhood.containsKey(it) }.flatMap { dir ->
                        connections.shuffled().filter {
                            it != node && it.wayId != node.wayId && it.neighborhood[dir.opposite()] is ConnectorNode
                        }
                    }.distinct()
                }
                val connectorNode = ConnectorNode(new, node.x, node.y, node.maxSpeed, NodeId(), node.wayId)
                Direction.values().filter { !node.neighborhood.containsKey(it) }
                    .forEach { node.neighborhood[it] = connectorNode }
            }
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