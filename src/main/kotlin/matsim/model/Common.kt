package matsim.model

import java.util.*
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract


fun createId() = UUID.randomUUID().toString()

operator fun Pair<Double,Double>.div(c: Int) = first.div(c) to second.div(c)
fun Lane.swapPart(part: Lane) : Lane  {
    val idNodeMap = part.associateBy { it.id }
    return map {
        idNodeMap[it.id] ?: it
    }
}
typealias Lane = List<Node>


@ExperimentalContracts
fun Any?.isNotNull() : Boolean {
    contract {
        returns(true) implies (this@isNotNull != null)

    }
    return this != null
}
