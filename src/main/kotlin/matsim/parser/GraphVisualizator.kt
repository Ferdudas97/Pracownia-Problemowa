package matsim.parser

import matsim.model.ConnectorNode
import matsim.model.Node
import org.graphstream.graph.Edge
import org.graphstream.graph.implementations.AbstractNode
import org.graphstream.graph.implementations.DefaultGraph
import org.graphstream.graph.implementations.SingleGraph
import java.util.*

fun graphToFile(nodes: List<Node>) {

    val graph = DefaultGraph("Droga", false, true)
    nodes.forEach {
        graph.addNode<AbstractNode>(it.id.id)
        it.neighborhood.values.filterIsInstance(ConnectorNode::class.java)
            .forEach { graph.addNode<AbstractNode>(it.id.id) }
    }
    nodes.forEach { it.toGraph(graph) }
    graph.display()


}

private fun Node.toGraph(graph: SingleGraph) {
    try {

        neighborhood.values.forEach {
            graph.addEdge<Edge>(UUID.randomUUID().toString(), id.id, it.id.id, true)
        }
        neighborhood.values.filterIsInstance(ConnectorNode::class.java).forEach { c ->
            c.getAll().forEach { graph.addEdge<Edge>(UUID.randomUUID().toString(), c.id.id, it.id.id) }
        }
    } catch (e: Exception) {
        println(e)
    }
}