package matsim.simulation

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import matsim.model.Node
import matsim.model.OccupiedNode
import matsim.model.swapPart

interface Simulation {

    fun start(): Simulation
    fun stop(): Simulation
}


class NSSimulation(val config: SimulationConfig) : Simulation {
    override fun start(): Simulation {
        val nodes = config.nodes;
        nodes.filterIsInstance(OccupiedNode::class.java)
            .groupBy(OccupiedNode::vehicle)
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun stop(): Simulation {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}