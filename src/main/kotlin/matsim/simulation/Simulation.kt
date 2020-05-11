package matsim.simulation

import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import matsim.model.*
import matsim.simulation.events.Event
import kotlin.math.min
import kotlin.system.measureTimeMillis

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
            repeat(config.steps) { step ->
                val ms = measureTimeMillis {
                    val result = nodeList.asFlow()
                        .filterIsInstance<OccupiedNode>()
                        .map { it.vehicle to createAnalyzableArea(it) }
                        .map { it.first.move(it.second) }
                        .buffer()
                        .onEach { resultReceiverActor.send(Event.Vehicle.Moved(it)) }
                        .toSet()

                    nodeList = result + nodeList.filterIsInstance(OccupiedNode::class.java)
                        .map(OccupiedNode::release) +
                            nodeList.filterIsInstance(TrafficLightNode::class.java).map { it.changePhase(step) } + nodeList
                }
                resultReceiverActor.send(Event.Simulation.StepDone(step, ms))
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
                ?.getNeighboursInDirection(areaSize) ?: emptyList()
        }
        return AnalyzableArea(map, occupiedNode)
    }

    override suspend fun stop() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}