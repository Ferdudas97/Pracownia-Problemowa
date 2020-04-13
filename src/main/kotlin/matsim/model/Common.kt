package matsim.model

import java.math.RoundingMode
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

fun <A, B> Pair<A, B>.reverse(): Pair<B, A> = second to first

fun Double.round(scale: Int = 7) = toBigDecimal().setScale(scale, RoundingMode.UP).toDouble()
@ExperimentalContracts
fun Any?.isNotNull(): Boolean {
    contract {
        returns(true) implies (this@isNotNull != null)

    }
    return this != null
}
