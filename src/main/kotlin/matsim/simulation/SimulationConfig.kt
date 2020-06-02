package matsim.simulation

import matsim.model.Node
import matsim.navigation.osrm.NavigationService

data class SimulationConfig(val nodes: List<Node>, val navigator: NavigationService, val steps: Int, val carNumber: Int)
