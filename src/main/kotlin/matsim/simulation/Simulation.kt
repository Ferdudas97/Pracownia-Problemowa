package matsim.simulation

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import matsim.model.Node

interface Simulation {

    fun start(): Simulation
    fun stop(): Simulation
}


class NSSimulation(val config: SimulationConfig) : Simulation {
    override fun start(): Simulation {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.

    }

    override fun stop(): Simulation {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}