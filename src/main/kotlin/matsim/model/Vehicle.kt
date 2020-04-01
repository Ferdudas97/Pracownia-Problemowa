package matsim.model

typealias Speed = Double

sealed class Vehicle {
    abstract val id: VehicleId
    abstract val maxSpeed: Speed
    abstract val acceleration: Speed
    abstract val length: Int

    abstract fun move()
}
inline class VehicleId(val value:String = createId())

data class Car(
    override val id: VehicleId,
    override val maxSpeed: Speed,
    override val acceleration: Speed,
    override val length: Int
) : Vehicle() {

    override fun move() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}


data class Truck(
    override val id: VehicleId,
    override val maxSpeed: Speed,
    override val acceleration: Speed,
    override val length: Int
) : Vehicle() {

    override fun move() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}