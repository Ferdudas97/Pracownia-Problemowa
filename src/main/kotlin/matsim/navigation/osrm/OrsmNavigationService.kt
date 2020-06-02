package matsim.navigation.osrm

import com.google.gson.Gson
import matsim.model.BasicNode
import matsim.model.Node
import matsim.model.NodeId
import matsim.navigation.model.Step
import matsim.navigation.osrm.data.NavResponse
import matsim.parser.NodeMapper
import matsim.parser.OsmParser
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import kotlin.contracts.ExperimentalContracts

const val apiUrl = "http://localhost:5000/route/v1/driving/"

class OrsmNavigationService(private val nodes: List<Node>) : NavigationService {
    private val client = OkHttpClient()

    override fun getRoute(
        source: Node,
        target: Node
    ): List<Node> {
        val url: String =
            createUrlWithQueryParams(source, target)
        val request = Request.Builder().url(url).build()
        val response = deserializeResponse(client.newCall(request).execute())
        val route = response.routes.map { it.legs }
            .map { it.first() }
            .flatMap { it.steps }
            .map { osrmStep ->
                Step(
                    osrmStep.maneuver.locationCoordinates[0],
                    osrmStep.maneuver.locationCoordinates[1],
                    osrmStep.durationSeconds,
                    osrmStep.distance
                )
            }
        return route.map { findClosest(it) }

    }

    private fun findClosest(step: Step) = nodes.find { it.x == step.latitude && it.y == step.longitude }
        ?: nodes.minBy { it.computeDistance(step.latitude, step.longitude) }!!

    private fun deserializeResponse(response: Response): NavResponse {
        val gson = Gson();
        return gson.fromJson(response.body()!!.string(), NavResponse::class.java)
    }


    private fun createUrlWithQueryParams(
        source: Node,
        target: Node
    ): String {
        val urlBuilder =
            HttpUrl.parse("$apiUrl${source.y},${source.x};${target.y},${target.x}")!!
                .newBuilder()
        urlBuilder.addQueryParameter("alternatives", "false")
        urlBuilder.addQueryParameter("steps", "true")
        return urlBuilder.build().toString()
    }
}

@ExperimentalContracts
private val parser = OsmParser()
@UseExperimental(ExperimentalContracts::class)
private val nodeMapper = NodeMapper()

@ExperimentalContracts
fun main() {
    val osmWays = parser.parse("/export.json")
    val nodes = nodeMapper.createSimulationWay(osmWays).flatMap { it.lanes() }.flatten()
    val r =
        OrsmNavigationService(nodes).getRoute(createNode(50.0491893, 19.9041208), createNode(50.0893397, 19.9743985))
    println(r)
}

private fun createNode(x: Double, y: Double) = BasicNode(NodeId(), x, y, maxSpeed = 0.0, wayId = "23", osmId = "@3")