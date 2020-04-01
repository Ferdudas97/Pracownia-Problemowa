package matsim.model


typealias Coordinate = Double


enum class NeighborhoodDirection {
    TOP, LEFT, RIGHT, BOTTOM
}

sealed class Node {
    abstract val x: Coordinate
    abstract val y: Coordinate
    abstract val neighborhood: Map<NeighborhoodDirection, Node>
    abstract val maxSpeed: Speed
    abstract val id: NodeId;
}
inline class NodeId(val id: String = createId())

data class BasicNode(
    override val id: NodeId,
    override val x: Coordinate,
    override val y: Coordinate,
    override val neighborhood: Map<NeighborhoodDirection, Node>,
    override val maxSpeed: Speed
) : Node()

data class OccupiedNode(
    val vehicle: Vehicle,
    override val id: NodeId,
    override val x: Coordinate,
    override val y: Coordinate,
    override val neighborhood: Map<NeighborhoodDirection, Node>,
    override val maxSpeed: Speed
) : Node()

data class TrafficLightNode(
    val active: Boolean,
    override val x: Coordinate,
    override val y: Coordinate,
    override val neighborhood: Map<NeighborhoodDirection, Node>,
    override val maxSpeed: Speed,
    override val id: NodeId
): Node()

fun BasicNode.occupyBy(vehicle: Vehicle): OccupiedNode {
    return OccupiedNode(vehicle,id, x, y, neighborhood, maxSpeed)
}

fun OccupiedNode.release(): BasicNode {
    return BasicNode(id,x, y, neighborhood, maxSpeed)
}

