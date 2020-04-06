package matsim.view

import de.saring.leafletmap.*
import matsim.parser.OsmParser
import org.controlsfx.control.WorldMapView
import tornadofx.*
import tornadofx.controlsfx.worldmapView
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
        cfMapLoadState.whenComplete{ w,_ ->
            mapView.setView(LatLong(50.06,19.09),12)

        }.whenComplete {
            w,_ ->
            showNodes()

        }
        add(mapView)


    }

    private fun showNodes() {
       val list = parser.parse("/export.json")
        print(list.size)
        list.filter { it.isNotEmpty() }.take(5000).forEach {
            mapView.addTrack(it.map {  node ->  LatLong(node.x,node.y)})
        }
    }
}
