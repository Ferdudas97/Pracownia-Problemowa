package matsim.navigation.model

data class Step(
    val longitude: Double,
    val latitude: Double,
    val deltaSeconds: Double,
    val distance: Double
)