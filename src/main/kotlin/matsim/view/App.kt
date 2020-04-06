package matsim.view

import javafx.application.Application
import tornadofx.App
import tornadofx.UIComponent
import tornadofx.launch
import kotlin.contracts.ExperimentalContracts
import kotlin.reflect.KClass

@ExperimentalContracts
class SimApp :  App(SimMapView::class)


@ExperimentalContracts
fun main(args: Array<String>) {
    launch<SimApp>(args)
}