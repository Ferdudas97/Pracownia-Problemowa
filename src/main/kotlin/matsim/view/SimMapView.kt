package matsim.view

import de.saring.leafletmap.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import matsim.model.Node
import matsim.parser.OsmParser
import org.controlsfx.control.WorldMapView
import tornadofx.*
import tornadofx.controlsfx.worldmapView
import kotlinx.coroutines.javafx.JavaFx as Main
import kotlin.contracts.ExperimentalContracts

@ExperimentalContracts
class SimMapView : View("My View") {
    private val parser = OsmParser()
    private val mapView = LeafletMapView()

    override val root = vbox {
        val cfMapLoadState = mapView.displayMap(
            MapConfig(
                layers = MapLayer.values().asList(),
                zoomControlConfig = ZoomControlConfig(true, ControlPosition.BOTTOM_LEFT),
                scaleControlConfig = ScaleControlConfig(true, ControlPosition.BOTTOM_LEFT, metric = true)
            )
        )

        add(mapView)

        cfMapLoadState.whenComplete { w, _ ->
            mapView.setView(LatLong(50.06, 19.09), 12)
            GlobalScope.launch(Dispatchers.Main) {
                showNodes().collect {
                    mapView.addTrack(it)
                }
            }

        }

    }

    private fun showNodes(): Flow<List<LatLong>> {
        val list = parser.parse("/export.json")
        return list.asFlow().filter { it.isNotEmpty() }.take(5000).map {
            it.map { node -> LatLong(node.x, node.y) }
        }
            .onEach { delay(10) }
            .buffer()
            .flowOn(Dispatchers.Default)
    }
}
