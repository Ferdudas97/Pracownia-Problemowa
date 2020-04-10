package matsim.view

import de.saring.leafletmap.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import matsim.model.Node
import matsim.model.OccupiedNode
import matsim.model.VehicleId
import matsim.parser.OsmParser
import matsim.simulation.NSSimulation
import matsim.simulation.SimulationConfig
import matsim.simulation.events.Event
import tornadofx.*
import kotlin.contracts.ExperimentalContracts

@ExperimentalContracts
class SimMapView : View("My View") {
    private val parser = OsmParser()
    private val mapView = LeafletMapView()

    override val root = borderpane() {
        val cfMapLoadState = mapView.displayMap(
            MapConfig(
                layers = MapLayer.values().asList(),
                zoomControlConfig = ZoomControlConfig(true, ControlPosition.BOTTOM_LEFT),
                scaleControlConfig = ScaleControlConfig(true, ControlPosition.BOTTOM_LEFT, metric = true)
            )
        )
        center {
            add(mapView)
            cfMapLoadState.whenComplete { w, _ ->
                mapView.setView(LatLong(50.061859, 19.938241), 12)
            }

        }

        left {
            vbox {
                button("Start") {
                    action {
                        GlobalScope.launch(Dispatchers.Main) {
                            if (cfMapLoadState.isDone) {
                                val nodes = parseNodes()
//                                nodes
//                                    .map { it.map { node -> LatLong(node.x, node.y) } }.collect { mapView.addTrack(it) }

                                val nodesList = nodes.flatMapMerge { it.asFlow() }
                                    .toList()
                                val simulation = SimulationConfig(nodesList, 10, 10)
                                NSSimulation(simulation, simulationViewActor(mapView)).start()
                            }

                        }
                    }
                }
            }
        }
    }


    private fun CoroutineScope.simulationViewActor(map: LeafletMapView) = actor<Event> {
        val markerMap = mutableMapOf<VehicleId, String>()
        fun OccupiedNode.putMarker() {
            val name = map.addMarker(LatLong(x, y), vehicle.id.value, ColorMarker.BLACK_MARKER, 5)
            markerMap[vehicle.id] = name
        }
        for (msg in channel) { // iterate over incoming messages
            when (msg) {
                is Event.Vehicle.Created -> msg.occupiedNode.apply {
                    putMarker()
                }
                is Event.Vehicle.Moved -> msg.occupiedNode.apply {
                    map.removeMarker(markerMap[vehicle.id]!!)
                    putMarker()

                }
            }
        }

    }


    private fun parseNodes(): Flow<List<Node>> {
        val list = parser.parse("/export.json")
        return list.asFlow().filter { it.isNotEmpty() }.take(5000)
//            .onEach { delay(10) }
            .buffer()
            .flowOn(Dispatchers.Default)
    }
}