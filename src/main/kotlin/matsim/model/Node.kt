package matsim.model

import java.time.Duration


typealias Coordinate = Double


enum class Direction {
    TOP, LEFT, RIGHT, BOTTOM
}

sealed class Node {
    abstract val x: Coordinate
    abstract val y: Coordinate
    abstract val neighborhood: Map<Direction, Node>
    abstract val maxSpeed: Speed
    abstract val id: NodeId;

    abstract fun addNeighbour(direction: Direction, node: Node): Node


}
inline class NodeId(val id: String = createId())

data class BasicNode(
    override val id: NodeId,
    override val x: Coordinate,
    override val y: Coordinate,
    override val neighborhood: Map<Direction, Node> = mapOf(),
    override val maxSpeed: Speed
) : Node() {
    override fun addNeighbour(direction: Direction, node: Node): BasicNode {
        return copy(neighborhood = neighborhood.plus(direction to node))
    }
}

data class OccupiedNode(
    val vehicle: Vehicle,
    override val id: NodeId,
    override val x: Coordinate,
    override val y: Coordinate,
    override val neighborhood: Map<Direction, Node>,
    override val maxSpeed: Speed
) : Node() {
    override fun addNeighbour(direction: Direction, node: Node): OccupiedNode {
        return copy(neighborhood = neighborhood.plus(direction to node))
    }

}

enum class TrafficPhase {
    GREEN, RED
}


data class TrafficLightNode(
    val phase: TrafficPhase = TrafficPhase.RED,
    val phaseTime : Map<TrafficPhase,Duration> = mapOf(),
    override val x: Coordinate,
    override val y: Coordinate,
    override val neighborhood: Map<Direction, Node> = mapOf(),
    override val maxSpeed: Speed,
    override val id: NodeId
): Node() {
    override fun addNeighbour(direction: Direction, node: Node): TrafficLightNode {
        return copy(neighborhood = neighborhood.plus(direction to node))
    }

}

fun Node.occupyBy(vehicle: Vehicle): OccupiedNode {
    return OccupiedNode(vehicle,id, x, y, neighborhood, maxSpeed)
}

fun OccupiedNode.release(): BasicNode {
    return BasicNode(id,x, y, neighborhood, maxSpeed)
}

