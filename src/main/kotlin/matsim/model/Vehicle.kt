package matsim.model

import kotlin.math.min
import kotlin.random.Random

typealias Speed = Double

data class Vehicle(
    val id: VehicleId = VehicleId(),
    val currentSpeed: Speed = 0.0,
    val maxSpeed: Speed = 5.0,
    val acceleration: Speed = 1.0,
    val destinationNodes: List<NodeId> = listOf(),
    val movementDirection: Direction
) {
    fun move(analyzableArea: AnalyzableArea): OccupiedNode {
        if (Random.nextDouble(0.0, 1.0) < 0.5) {
            val (fastestSpeed, fastestDirection) = findFastestAreaDirection(analyzableArea)
            val chosenLane = analyzableArea.nodesByDirection(fastestDirection)
            val updatedVehicle = copy(currentSpeed = fastestSpeed)
            val newOccupiedNode =
                chosenLane.getOrNull(min(fastestSpeed.toInt() - 1, chosenLane.size - 1)) ?: analyzableArea.currentNode
            return newOccupiedNode.occupyBy(updatedVehicle)
        } else {
            val selectedDirection =
                movementDirection.getPossibleDirections().filter { it == movementDirection }.shuffled().first()
            val lane = analyzableArea.nodesByDirection(selectedDirection)
            val speed = lane.computeAvailableSpeed()
            val updatedVehicle = copy(currentSpeed = speed)
            val newOccupiedNode = lane.getOrNull(speed.toInt() - 1) ?: analyzableArea.currentNode
            return newOccupiedNode.occupyBy(updatedVehicle)
        }

    }

    private fun findFastestAreaDirection(analyzableArea: AnalyzableArea): Pair<Speed, Direction> {

        return movementDirection.getPossibleDirections()
            .map { analyzableArea.nodesByDirection(it) to it }
            .map {
                it.first.computeAvailableSpeed() to it.second
            }
            .maxBy { it.first } ?: 0.0 to movementDirection
    }

    private fun List<Node>.availableDistance() =
        mapIndexed { index, node -> if (node is OccupiedNode) index else size - 1 }.max() ?: size - 1

    fun List<Node>.computeAvailableSpeed(): Speed = if (this.isEmpty()) 0.0 else listOf(
        currentSpeed + acceleration,
        maxSpeed,
        first().maxSpeed,
        availableDistance().toDouble()
    ).min()!!
}

data class AnalyzableArea(private val nodeMap: Map<Direction, Lane>, val currentNode: OccupiedNode) {
    fun nodesByDirection(direction: Direction) = nodeMap[direction] ?: emptyList()
    fun changeLane(direction: Direction, lane: Lane): AnalyzableArea {
        val newMap = nodeMap.filterKeys { it != direction }.plus(direction to lane)
        return copy(nodeMap = newMap)
    }
}


inline class VehicleId(val value: String = createId())

