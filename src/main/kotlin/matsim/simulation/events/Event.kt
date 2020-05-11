package matsim.simulation.events

import matsim.model.OccupiedNode


sealed class Event {
    sealed class Simulation : Event() {
        object Started : Simulation()
        object Finished : Simulation()
        data class StepDone(val step: Int, val time: Long) : Simulation()
    }

    sealed class Vehicle : Event() {
        data class Moved(val occupiedNode: OccupiedNode) : Vehicle()
        data class Created(val occupiedNode: OccupiedNode) : Vehicle()
    }

}

