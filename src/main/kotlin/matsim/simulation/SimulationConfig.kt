package matsim.simulation

import matsim.model.Node

data class SimulationConfig(val nodes: List<Node>, val steps: Int, val carNumber: Int)
