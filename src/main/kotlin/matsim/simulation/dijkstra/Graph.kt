package matsim.simulation.dijkstra

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import matsim.model.Node
import matsim.parser.OsmWay
import java.util.concurrent.ConcurrentHashMap


class RoadNavigator(private val graph: Graph<String>) {

    private val dijkstras: MutableMap<String, Map<String, String?>> = ConcurrentHashMap()

    private fun getDijkstra(start: Node): Map<String, String?> {

        return dijkstras.getOrPut(start.osmId) { dijkstra(graph, start.osmId) }
    }

    fun getRoad(start: Node, end: Node) = shortestPath(getDijkstra(start), start.osmId, end.osmId)
}

data class Graph<T>(
    val vertices: Set<T>,
    val edges: Map<T, Set<T>>,
    val weights: Map<Edge<T>, Int>
)

suspend fun createGraph(ways: List<OsmWay>): Graph<String> {
    val chunked = ways.flatMap { way -> way.nodes.edge() }
    val graphPrototype = chunked.flatMap { pair ->
        listOf(pair.first to chunked.mapNotNull { it.getOther(pair.first) },
            pair.second to chunked.mapNotNull { it.getOther(pair.second) })
    }.groupBy { it.first }
        .mapValues { groupped ->
            groupped.value.flatMap { it.second }.toSet()
        }
    val vertices = GlobalScope.async { graphPrototype.keys.map { it.id }.toSet() }
    val edges =
        GlobalScope.async { graphPrototype.mapKeys { it.key.id }.mapValues { it.value.map { l -> l.id }.toSet() } }
    val weights = GlobalScope.async {
        chunked.associateWith { it.first.computeDistance(it.second).toInt() }
            .mapKeys { Edge(it.key.first.id, it.key.second.id) }
    }
    return Graph(vertices.await(), edges.await(), weights.await())
}

private fun <T> Pair<T, T>.getOther(o: T) = when (o) {
    this.first -> this.second
    this.second -> this.first
    else -> null
}

private fun <T> dijkstra(graph: Graph<T>, start: T): Map<T, T?> {
    val S: MutableSet<T> = mutableSetOf() // a subset of vertices, for which we know the true distance

    val delta = graph.vertices.map { it to Int.MAX_VALUE }.toMap().toMutableMap()
    delta[start] = 0

    val previous: MutableMap<T, T?> = graph.vertices.map { it to null }.toMap().toMutableMap()

    while (S != graph.vertices) {
        try {

            val v: T = delta
                .filter { !S.contains(it.key) }
                .minBy { it.value }!!
                .key

            graph.edges.getValue(v).minus(S).forEach { neighbor ->
                val newPath = delta.getValue(v) + graph.weights.getWeight(Edge(v, neighbor))

                if (newPath < delta.getValue(neighbor)) {
                    delta[neighbor] = newPath
                    previous[neighbor] = v
                }
            }

            S.add(v)
        } catch (e: Throwable) {
            println(e)
        }
    }

    return previous.toMap()
}

private fun <T> Map<Edge<T>, Int>.getWeight(key: Edge<T>) =
    get(key) ?: getOrDefault(Edge(key.b, key.a), Int.MAX_VALUE)

private fun <T> shortestPath(shortestPathTree: Map<T, T?>, start: T, end: T): List<T> {
    fun pathTo(start: T, end: T): List<T> {
        if (shortestPathTree[end] == null) return listOf(end)
        return listOf(pathTo(start, shortestPathTree[end]!!), listOf(end)).flatten()
    }

    return pathTo(start, end)
}

data class Edge<T>(val a: T, val b: T) {
    override fun equals(other: Any?): Boolean {
        return if (other == null) false else {
            if (other is Edge<*>) {
                (a == other.a && b == other.b) || (a == other.b && b == other.a)
            } else false
        }
    }

    override fun hashCode(): Int {
        return super.hashCode()
    }
}

private fun <T> List<T>.edge() = mapIndexedNotNull { index, t ->
    if (index == 0) null else get(index - 1) to get(index)
}


fun main() {
    val l = listOf(1, 2, 3, 4, 5, 6, 7).chunked(2)
    println(l)
}