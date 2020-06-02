package matsim.navigation.osrm

import matsim.model.Node


interface NavigationService {
    fun getRoute(
        source: Node,
        target: Node
    ): List<Node>

}