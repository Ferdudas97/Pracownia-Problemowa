package matsim.view

import javafx.beans.property.SimpleLongProperty
import tornadofx.ViewModel


class SimViewModel : ViewModel() {
    val carNumber = SimpleLongProperty()
    val stepsNumber = SimpleLongProperty()
}