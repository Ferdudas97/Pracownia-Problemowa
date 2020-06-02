package matsim.navigation.osrm.data

import com.google.gson.annotations.SerializedName

data class NavResponse(
    @SerializedName("code")
    val code: String,
    @SerializedName("routes")
    val routes: List<Route>,
    @SerializedName("waypoints")
    val waypoints: List<Waypoint>

)

data class Maneuver(
    @SerializedName("location")
    val locationCoordinates: List<Double>
)

data class OsrmStep(
    @SerializedName("duration")
    val durationSeconds: Double,

    @SerializedName("maneuver")
    val maneuver: Maneuver,

    @SerializedName("distance")
    val distance: Double
)

data class StepsContainer(
    @SerializedName("steps")
    val steps: List<OsrmStep>
)

data class Route(
    @SerializedName("geometry")
    val geometry: String,
    @SerializedName("legs")
    val legs: List<StepsContainer>,
    @SerializedName("distance")
    val distance: Double,
    @SerializedName("duration")
    val durationSeconds: Double,
    @SerializedName("weight")
    val weight: Double
)

data class Waypoint(
    @SerializedName("hint")
    val hint: String,

    @SerializedName("distance")
    val distance: Double,

    @SerializedName("name")
    val name: String,

    @SerializedName("location")
    val locationCoordinates: List<Double>
)