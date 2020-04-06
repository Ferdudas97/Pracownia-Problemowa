package matsim

import com.beust.klaxon.Parser
import matsim.parser.OsmParser
import kotlin.contracts.ExperimentalContracts


@ExperimentalContracts
fun main() {
    val parser = OsmParser()
    parser.parse("/export.json").size.run { println(this) }
}





