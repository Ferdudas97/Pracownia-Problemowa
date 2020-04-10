package matsim.model

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
        val (fastestSpeed, fastestDirection) = findFastestAreaDirection(analyzableArea)
        val chosenLane = analyzableArea.nodesByDirection(fastestDirection)
        val updatedVehicle = copy(currentSpeed = fastestSpeed)
        val newOccupiedNode = chosenLane.getOrNull(fastestSpeed.toInt()) ?: analyzableArea.currentNode
        return newOccupiedNode.occupyBy(updatedVehicle)
    }

    private fun findFastestAreaDirection(analyzableArea: AnalyzableArea): Pair<Speed, Direction> {

        return movementDirection.getPossibleDirections()
            .map { analyzableArea.nodesByDirection(it) to it }
            .map { computeAvailableSpeed(it.first.firstOrNull()?.maxSpeed ?: 0.0) to it.second }
            .maxBy { it.first } ?: 0.0 to movementDirection
    }

    fun computeAvailableSpeed(nodeMaxSpeed: Speed): Speed =
        listOf(currentSpeed + acceleration, maxSpeed, nodeMaxSpeed).min()!!
}

data class AnalyzableArea(private val nodeMap: Map<Direction, Lane>, val currentNode: OccupiedNode) {
    fun nodesByDirection(direction: Direction) = nodeMap[direction] ?: emptyList()
    fun changeLane(direction: Direction, lane: Lane): AnalyzableArea {
        val newMap = nodeMap.filterKeys { it != direction }.plus(direction to lane)
        return copy(nodeMap = newMap)
    }
}


inline class VehicleId(val value: String = createId())

