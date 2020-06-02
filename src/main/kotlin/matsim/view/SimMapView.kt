package matsim.view

import de.saring.leafletmap.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.launch
import matsim.model.OccupiedNode
import matsim.model.VehicleId
import matsim.navigation.osrm.OrsmNavigationService
import matsim.parser.NodeMapper
import matsim.parser.OsmParser
import matsim.simulation.NSSimulation
import matsim.simulation.SimulationConfig
import matsim.simulation.events.Event
import tornadofx.*
import kotlin.contracts.ExperimentalContracts

@ExperimentalContracts
class SimMapView : View("My View") {
    private val parser = OsmParser()
    private val nodeMapper = NodeMapper()
    private val mapView = LeafletMapView()
    private val viewModel: SimViewModel by inject()

    @ExperimentalStdlibApi
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
                                val viewActor = simulationViewActor(mapView)
                                launch(Dispatchers.IO) {
                                    val config = createConfig()
                                    NSSimulation(config, viewActor).start()

                                }
                            }

                        }
                    }
                }
            }
        }
    }


    @UseExperimental(ExperimentalStdlibApi::class)
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
                    println("lat = $x long = $y id= $id vehicle = ${vehicle.id.value}")
                    map.moveMarker(markerMap[vehicle.id]!!, LatLong(x, y))
                }
                is Event.Simulation.StepDone -> println(msg)
            }
        }

    }


    private suspend fun createConfig(): SimulationConfig {
        val osmWays = parser.parse("/export.json")
        val nodes = nodeMapper.createSimulationWay(osmWays).flatMap { it.lanes() }.flatten()
        return SimulationConfig(nodes, OrsmNavigationService(nodes), 3000, 1)
    }
}