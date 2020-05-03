package matsim.simulation

import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import matsim.model.*
import matsim.simulation.events.Event
import kotlin.math.min

interface Simulation {

    suspend fun start()
    suspend fun stop()
}


class NSSimulation(private val config: SimulationConfig, private val resultReceiverActor: SendChannel<Event>) :
    Simulation {
    lateinit var nodeList: Set<Node>
    override suspend fun start() {
        val nodes = config.nodes;
        nodeList = nodes.toSet()
        coroutineScope {
            val cars = createCars(config.carNumber)
            nodeList = cars + nodeList
            repeat(config.steps) {
                delay(500)
                val result = nodeList.asFlow()
                    .filterIsInstance<OccupiedNode>()
                    .map { it.vehicle to createAnalyzableArea(it) }
                    .map { it.first.move(it.second) }
                    .buffer()
                    .onEach { resultReceiverActor.send(Event.Vehicle.Moved(it)) }
                    .toSet()

                nodeList = result + nodeList.filterIsInstance(OccupiedNode::class.java)
                    .map(OccupiedNode::release) + nodeList
            }
        }
    }

    private suspend fun createCars(number: Int): Set<OccupiedNode> = nodeList.shuffled()
        .asFlow()
        .take(number)
        .map { it.occupyBy(Vehicle(movementDirection = Direction.RIGHT, destination = nodeList.random())) }
        .onEach { resultReceiverActor.send(Event.Vehicle.Created(it)) }
        .toSet()


    private fun createAnalyzableArea(occupiedNode: OccupiedNode): AnalyzableArea {
        val direction = occupiedNode.vehicle.movementDirection
        val areaSize = min(occupiedNode.vehicle.maxSpeed, occupiedNode.maxSpeed).toInt()
        val directions = direction.getPossibleDirections()
        val map = directions.associateWith {
            occupiedNode
                .neighborhood[it]
                .getNeighboursInDirection(direction, areaSize)
        }
        return AnalyzableArea(map, occupiedNode)
    }

    private fun Node?.getNeighboursInDirection(direction: Direction, number: Int): List<Node> {
        val list = mutableListOf<Node>()
        var node = this
        repeat(number) {
            if (node != null) {
                list.add(node!!)
                node = node?.neighborhood?.get(direction)
            }

        }
        return list
    }


    override suspend fun stop() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}