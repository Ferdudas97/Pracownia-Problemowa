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
    private val viewModel: SimViewModel by inject()

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
                label("Simulation Steps")
                textfield(viewModel.stepsNumber)
                separator()
                label("Car Number")
                textfield(viewModel.carNumber)
                separator()
                button("Start") {
                    action {
                        GlobalScope.launch(Dispatchers.Main) {
                            if (cfMapLoadState.isDone) {
                                val nodes = parseNodes()
//                                nodes
//                                    .map { it.map { node -> LatLong(node.x, node.y) } }
//                                    .collect { mapView.addTrack(it) }

                                val nodesList = nodes.flatMapMerge { it.asFlow() }
                                    .toList()
                                val simulation = SimulationConfig(nodesList, 300, 25000)
                                val viewActor = simulationViewActor(mapView)
                                launch(Dispatchers.IO) {
                                    NSSimulation(simulation, viewActor).start()

                                }
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
                    //                    putMarker()
                }
                is Event.Vehicle.Moved -> msg.occupiedNode.apply {
                    //                    println("lat = $x long = $y id= $id vehicle = ${vehicle.id.value}")
//                    map.moveMarker(markerMap[vehicle.id]!!, LatLong(x, y))
                }
                is Event.Simulation.StepDone -> println(msg)
            }
        }

    }


    private fun parseNodes(): Flow<List<Node>> {
        val list = parser.parse("/export.json")
        return list.asFlow().flatMapMerge { it.lanes().asFlow() }.filter { it.isNotEmpty() }
//            .onEach { delay(10) }
            .buffer()
            .flowOn(Dispatchers.Default)
    }
}