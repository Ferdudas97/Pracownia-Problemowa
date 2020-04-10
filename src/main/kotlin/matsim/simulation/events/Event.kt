package matsim.simulation.events

import matsim.model.OccupiedNode


sealed class Event {
    sealed class Simulation : Event() {
        object Started : Simulation()
        object Finished : Simulation()
    }

    sealed class Vehicle : Event() {
        data class Moved(val occupiedNode: OccupiedNode) : Vehicle()
        data class Created(val occupiedNode: OccupiedNode) : Vehicle()
    }

}

