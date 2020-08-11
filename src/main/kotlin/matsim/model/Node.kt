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
    abstract val osmId: String


    fun computeDistance(lat: Coordinate, long: Coordinate): Double {
        val latInRadians = x.toRadians()
        val longInRadians = y.toRadians()
        val otherLatInRadians = lat.toRadians()
        val otherLongInRadians = long.toRadians()
        val latDelta = latInRadians - otherLatInRadians
        val longDelta = longInRadians - otherLongInRadians
        val a =
            sin(latDelta / 2) * sin(latDelta / 2) + cos(latInRadians) * cos(otherLatInRadians) * sin(longDelta / 2) * sin(
                longDelta / 2
            )
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c
    }

    open fun getNextNode(destination: Node) = neighborhood.filterKeys { it != Direction.LEFT }
        .values.minBy {
        computeDistance(it, destination)
    }
        ?: neighborhood[Direction.RIGHT]!!

    private fun computeDistance(next: Node, destination: Node): Double = when (next) {
        is ConnectorNode -> next.getNextNode(destination).run {
            if (wayId == destination.wayId) Double.MIN_VALUE else computeDistance(destination)
        }
        else -> next.computeDistance(destination)
    }

    fun computeDistance(other: Node): Double {
        // algorithm from https://www.movable-type.co.uk/scripts/latlong.html
        return computeDistance(other.x, other.y)
    }

    fun getNeighboursInDirection(number: Int, route: Node, direction: Direction = Direction.RIGHT): List<Node> {
        var node: Node = this
        val result = mutableListOf<Node>()
        return runCatching {
            loop@ while (result.size < number) {
                if (node !is ConnectorNode && node !is TrafficLightNode) result.add(node)
                if (node.osmId == "288738490") {
                    println()
                }
                node = when {
                    node is ConnectorNode -> {
                        (node as ConnectorNode).getNextNode(route)
                    }
                    node.neighborhood[direction] != null -> {
                        node.getNextNode(route)

                    }
                    else -> break@loop
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
    override val maxSpeed: Speed, override val wayId: String, override val osmId: String
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

data class OccupiedNode @ExperimentalStdlibApi constructor(
    val vehicle: Vehicle,
    override val id: NodeId,
    override val x: Coordinate,
    override val y: Coordinate,
    override val neighborhood: MutableMap<Direction, Node> = mutableMapOf(),
    override val maxSpeed: Speed, override val wayId: String, override val osmId: String
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
    override val id: NodeId, override val wayId: String, override val osmId: String
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
    private val baseNeighborhood: MutableMap<Direction, Node>,
    override val x: Coordinate,
    override val y: Coordinate,
    override val maxSpeed: Speed,
    override val id: NodeId, override val wayId: String, override val osmId: String
) : Node() {
    override val neighborhood: MutableMap<Direction, Node>
        get() {
            val map = mutableMapOf<Direction, Node>()
            map.putAll(baseNeighborhood)
            val chosenNodes = nodes.shuffled().take(3)
            chosenNodes.getOrNull(0)?.let { map[Direction.RIGHT] = it }
            chosenNodes.getOrNull(1)?.let { map[Direction.TOP] = it }
            chosenNodes.getOrNull(2)?.let { map[Direction.BOTTOM] = it }
            map.remove(Direction.LEFT)
            return map
        }

    override fun equals(other: Any?): Boolean {
        return super.equals(other)
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }

    override fun toString(): String {
        return super.toString()
    }

    fun getAll() = nodes + neighborhood.values

    override fun getNextNode(destination: Node) = (nodes).filterNot { it is ConnectorNode }
        .filter { it.wayId == destination.wayId && it.osmId == destination.osmId }
        .minBy { it.neighborhood[Direction.RIGHT]?.computeDistance(destination) ?: Double.MAX_VALUE }
        ?: (nodes).filterNot { it is ConnectorNode }.groupBy {
            it.neighborhood[Direction.RIGHT]?.computeDistance(
                destination
            ) ?: Double.MAX_VALUE
        }
            .toSortedMap().values.takeIf { it.isNotEmpty() }?.let {
            val nodesWithMin = it.first()
            if (nodesWithMin.size == 1) nodesWithMin.firstOrNull()
            else nodesWithMin.minBy { it.getNextNode(destination).computeDistance(destination) }
        }
        ?: baseNeighborhood[Direction.RIGHT]!!

}

@ExperimentalContracts
class Connector {
    private val connectionsMap = mutableMapOf<String, List<Node>>()
    fun addConnection(id: String, node: Node) {
        connectionsMap[id] = connectionsMap.getOrDefault(id, listOf()).plus(node)
    }

    fun removeConnection(node: Node) {
        connectionsMap[node.osmId] = connectionsMap.getOrDefault(node.osmId, listOf()).minus(node)
    }

    fun connect() {
        connectionsMap.values.filter { it.size > 1 }.forEach(this::connect)

    }

    private fun connect(connections: List<Node>) {
//        if (connections.map(Node::wayId).distinct().size == 2) {
//            connections.forEach { node ->
//                val new = Direction.values().filter { !node.neighborhood.containsKey(it) }.map { dir ->
//                    dir to connections.shuffled().find {
//                        it != node && it.wayId != node.wayId
//                                && !it.neighborhood.containsKey(dir.opposite())
//                    }
//                }.associate { it }.filterValues { it != null }.mapValues { it.value!! }
//                new.filter { it.value.neighborhood[it.key.opposite()] == null }
//                    .forEach {
//                        it.value.neighborhood[it.key.opposite()] = node
//                        node.neighborhood[it.key] = it.value
//                    }
//                if (node.osmId == "288738490") {
//                    println(new)
//                }
//            }
//        } else {
        if (connections.first().osmId == "1245992910") {
            println("")
        }
        val (leftNull, leftRest) = connections.partition { !it.neighborhood.containsKey(Direction.LEFT) }
        val (rightNull, rightRest) = connections.partition { !it.neighborhood.containsKey(Direction.RIGHT) }
        rightRest.groupBy { it.wayId }.forEach { wayId, nodes ->
            val node = nodes.first()
            val connectorNode = ConnectorNode(
                leftNull,
                node.neighborhood,
                node.x,
                node.y,
                node.maxSpeed,
                NodeId(),
                node.wayId,
                node.osmId
            )
            nodes.forEach { n ->
                Direction.values().filter { !n.neighborhood.containsKey(it) }
                    .forEach { n.neighborhood[it] = connectorNode }
            }
        }
        leftNull.groupBy { it.wayId }.forEach { wayId, nodes ->
            val node = nodes.first()
            val connectorNode = ConnectorNode(
                leftRest,
                node.neighborhood,
                node.x,
                node.y,
                node.maxSpeed,
                NodeId(),
                node.wayId,
                node.osmId
            )
            nodes.forEach { n ->
                Direction.values().filter { !n.neighborhood.containsKey(it) }
                    .forEach { n.neighborhood[it] = connectorNode }
            }
        }

        rightNull.groupBy { it.wayId }.forEach { wayId, nodes ->
            val node = nodes.first()
            val connectorNode = ConnectorNode(
                rightRest,
                node.neighborhood,
                node.x,
                node.y,
                node.maxSpeed,
                NodeId(),
                node.wayId,
                node.osmId
            )
            nodes.forEach { n ->
                Direction.values().filter { !n.neighborhood.containsKey(it) }
                    .forEach { n.neighborhood[it] = connectorNode }
            }
        }
    }
}

//}


data class Way(
    val id: String,
    val oneWay: List<Lane> = emptyList(),
    val twoWay: List<Lane> = emptyList()
) {
    fun lanes() = oneWay + twoWay
}

@ExperimentalStdlibApi
fun Node.occupyBy(vehicle: Vehicle): OccupiedNode {
    return OccupiedNode(vehicle, id, x, y, neighborhood, maxSpeed, wayId, osmId)
}

fun OccupiedNode.release(): BasicNode {
    return BasicNode(id, x, y, neighborhood, maxSpeed, wayId, osmId)
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

private fun Boolean.toInt() = if (this) 1 else 0