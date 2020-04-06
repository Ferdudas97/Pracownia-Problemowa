package matsim.model

import javax.swing.DropMode

typealias Speed = Double

data class Vehicle(
    val id: VehicleId = VehicleId(),
    val currentSpeed: Speed = 0.0,
    val maxSpeed: Speed = 5.0,
    val acceleration: Speed = 1.0,
    val length: Int = 1,
    val destinationNodes: List<NodeId> = listOf(),
    val movementDirection: Direction
) {
    fun move(analyzableArea: AnalyzableArea): Pair<Vehicle,AnalyzableArea> {
        val (fastestSpeed, fastestDirection) = findFastestAreaDirection(analyzableArea)
        val chosenLane = analyzableArea.nodesByDirection(fastestDirection)
        val updatedVehicle = copy(currentSpeed = fastestSpeed)
        val newOccupiedNodes = chosenLane.take(fastestSpeed.toInt())
            .takeLast(length)
            .map { it.occupyBy(updatedVehicle) }
        val leftNodes = analyzableArea.nodesByDirection(movementDirection)
            .takeLast(length)
            .filterIsInstance(OccupiedNode::class.java)
            .map { it.release() }
        val newChosenLane = chosenLane.swapPart(newOccupiedNodes)
        val newLeftNode = analyzableArea.nodesByDirection(movementDirection.opposite())
            .swapPart(leftNodes)
        val newArea = analyzableArea.changeLane(movementDirection.opposite(), newLeftNode)
            .changeLane(fastestDirection, newChosenLane)
        return updatedVehicle to newArea
    }

    private fun findFastestAreaDirection(analyzableArea: AnalyzableArea): Pair<Speed, Direction> {

        return movementDirection.getPossibleDirections()
            .map { analyzableArea.nodesByDirection(it) to it }
            .map { computeAvailableSpeed(it.first.firstOrNull()?.maxSpeed ?: 0.0) to it.second }
            .maxBy { it.first }!!
    }

    fun computeAvailableSpeed(nodeMaxSpeed: Speed): Speed =
        listOf(currentSpeed + acceleration, maxSpeed, nodeMaxSpeed).min()!!
}

data class AnalyzableArea(private val nodeMap: Map<Direction, Lane>) {
    fun nodesByDirection(direction: Direction) = nodeMap[direction] ?: emptyList()
    fun changeLane(direction: Direction, lane: Lane): AnalyzableArea {
        val newMap = nodeMap.filterKeys { it != direction }.plus(direction to lane)
        return copy(nodeMap = newMap)
    }
}

private fun Direction.getPossibleDirections() = when (this) {
    Direction.LEFT, Direction.RIGHT -> setOf(Direction.BOTTOM, Direction.TOP, this)
    Direction.TOP, Direction.BOTTOM -> setOf(Direction.RIGHT, Direction.LEFT, this)
}

private fun Direction.opposite() = when (this) {
    Direction.TOP -> Direction.BOTTOM
    Direction.LEFT -> Direction.RIGHT
    Direction.BOTTOM -> Direction.TOP
    Direction.RIGHT -> Direction.LEFT
}

inline class VehicleId(val value: String = createId())

