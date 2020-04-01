package matsim.simulation.events

import matsim.model.VehicleId


sealed class Event {
    sealed class Simulation : Event() {
        object Started: Simulation()
        object Finished: Simulation()
    }
    sealed class Vehicle {
        data class Moved(val vehicleId: VehicleId) : Vehicle()
    }

}

