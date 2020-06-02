package matsim.model

import kotlin.math.min
import kotlin.random.Random

typealias Speed = Double

@ExperimentalStdlibApi
data class Vehicle(
    val id: VehicleId = VehicleId(),
    val currentSpeed: Speed = 0.0,
    val maxSpeed: Speed = 5.0,
    val acceleration: Speed = 1.0,
    val destination: Node,
    val route: MutableList<Node>,
    val lastNodes: MutableList<NodeId> = mutableListOf(),
    val movementDirection: Direction,
    private val currentRouteNode: Int = -1
) {
    fun move(analyzableArea: AnalyzableArea): OccupiedNode {
        if (Random.nextDouble(0.0, 1.0) < 1.4) {
            val (fastestSpeed, fastestDirection) = findFastestAreaDirection(analyzableArea)
            val chosenLane = analyzableArea.nodesByDirection(fastestDirection)
            val size = min(fastestSpeed.toInt() - 1, chosenLane.size - 1);
            val newOccupiedNode =
                chosenLane.getOrNull(size) ?: analyzableArea.currentNode
            val newCurrentRouteNode =
                if (chosenLane.take(size + 1).map { n -> n.osmId to n.wayId }.contains(nextNodeOnRoute().osmId to nextNodeOnRoute().wayId)) currentRouteNode + 1 else currentRouteNode

            addToLastNodes(newOccupiedNode)
            if (newCurrentRouteNode != currentRouteNode) {
                println(newCurrentRouteNode)
            }

            val updatedVehicle = copy(currentSpeed = fastestSpeed, currentRouteNode = newCurrentRouteNode)

            return newOccupiedNode.occupyBy(updatedVehicle)
        } else {
            val lane = randomLana(analyzableArea)
            val speed = lane.computeAvailableSpeed()
            val newOccupiedNode = lane.getOrNull(speed.toInt() - 1) ?: analyzableArea.currentNode
            addToLastNodes(newOccupiedNode)
            val newCurrentRouteNode = min(routeIds().indexOf(newOccupiedNode.osmId), currentRouteNode)
            if (newCurrentRouteNode != currentRouteNode) {
                println(newCurrentRouteNode)
            }
            val updatedVehicle = copy(currentSpeed = speed, currentRouteNode = newCurrentRouteNode)
            return newOccupiedNode.occupyBy(updatedVehicle)
        }

    }

    private fun addToLastNodes(node: Node) {
        lastNodes.add(node.id)
    }


    fun nextNodeOnRoute() = route.get(currentRouteNode + 1)
    private fun Node.distanceToDestination() = nextNodeOnRoute().computeDistance(this)

    private fun randomLana(analyzableArea: AnalyzableArea) =
        movementDirection.getPossibleDirections().map { analyzableArea.nodesByDirection(it) }
            .filter { it.isNotEmpty() }
            .filter { routeIds().contains(it.first().osmId) }.randomOrNull()
            ?: movementDirection.getPossibleDirections()
                .random().run { analyzableArea.nodesByDirection(this) }

    private fun findFastestAreaDirection(analyzableArea: AnalyzableArea): Pair<Speed, Direction> {
        val possibleLanes =
            movementDirection.getPossibleDirections()
                .map { analyzableArea.nodesByDirection(it) to it }
                .filter { it.first.isNotEmpty() }
                .groupBy { it.first[it.first.availableDistance()].distanceToDestination() }
                .minBy { it.key }?.value ?: emptyList()

        return possibleLanes
            .map {
                it.first.computeAvailableSpeed() to it.second
            }
            .maxBy { it.first } ?: 0.0 to movementDirection
    }

    fun routeIds() = route.map { it.osmId }
    fun nextRoute() = route[currentRouteNode + 1] ?: destination
    private fun List<Node>.isNotVisited() = map(Node::id).intersect(lastNodes).isEmpty()
    private fun List<Node>.availableDistance() =
        mapIndexed { index, node ->
            when (node) {
                is OccupiedNode -> index
                is TrafficLightNode -> if (node.phase == TrafficPhase.RED) index else size - 1
                else -> size - 1
            }
        }
            .max() ?: size - 1

    private fun List<Node>.computeAvailableSpeed(): Speed = if (this.isEmpty()) 0.0 else listOf(
        currentSpeed + acceleration,
        maxSpeed,
        availableDistance().toDouble()
    ).min()!!
}

@ExperimentalStdlibApi
data class AnalyzableArea(private val nodeMap: Map<Direction, Lane>, val currentNode: OccupiedNode) {
    fun nodesByDirection(direction: Direction): List<Node> {
        val nodes = nodeMap[direction] ?: emptyList()
        val delta = currentNode.maxSpeed - nodes.size
        return if (delta > 0 && nodes.isNotEmpty()) {
//            val last = nodes.last()
//            val new = last.getNeighboursInDirection(
//                delta.toInt(),
//                route = currentNode.vehicle.nextRoute()
//            )

//            nodes.plus(new)
            nodes
        } else {
            nodes
        }
    }

    fun changeLane(direction: Direction, lane: Lane): AnalyzableArea {
        val newMap = nodeMap.filterKeys { it != direction }.plus(direction to lane)
        return copy(nodeMap = newMap)
    }
}

inline class VehicleId(val value: String = createId())

