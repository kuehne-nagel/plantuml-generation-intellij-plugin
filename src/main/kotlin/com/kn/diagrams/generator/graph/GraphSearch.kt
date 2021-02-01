package com.kn.diagrams.generator.graph

import com.kn.diagrams.generator.toSingleList
import java.util.*


enum class EdgeMode { TypesOnly, MethodsOnly, TypesAndMethods, MethodsAndDirectTypeUsage }

class FindContext(private val cache: GraphCache,
                  private val direction: Direction,
                  private val filter: TraversalFilter,
                  private val depth: Int,
                  private val edgeMode: EdgeMode
) {

    fun find(root: GraphNode): List<List<SquashedGraphEdge>> {
        val callChains = mutableListOf<List<SquashedGraphEdge>>()

        val stack = Stack<GraphDirectedEdge>()
        val expandedNodes = mutableSetOf<GraphNode>()
        val processedEdges = mutableSetOf<GraphDirectedEdge>()
        val currentChain = mutableListOf<GraphDirectedEdge>()
        val currentSquashedChain = mutableListOf<SquashedGraphEdge>()

        root.navigate().forEach { stack.add(it) }

        while (stack.isNotEmpty()) {
            val current = stack.pop()
            processedEdges.add(current)

            cleanupChainsFromDeadEdges(current, currentChain, currentSquashedChain)

            val moreEdges = current.next()
                    .takeUnless { expandedNodes.contains(it) }
                    ?.let {
                        expandedNodes.add(it)
                        it
                    }
                    ?.navigate()
                    ?.filterNot { processedEdges.contains(it) }
                    ?.filter { edgeMode != EdgeMode.MethodsAndDirectTypeUsage
                            || it.context.none { it is MethodClassUsage }
                            || filter.accept(current.next())
                    }
                    ?: emptySequence()

            if (moreEdges.count() == 0 || currentSquashedChain.count { it.isComplete() } >= depth) {
                callChains.add(currentSquashedChain.filter { it.isComplete() }.apply(direction))
            } else {
                moreEdges
                        .filterNot { processedEdges.contains(it) }
                        .forEach { stack.add(it) }
            }
        }

        return callChains.toList()
    }

    private fun cleanupChainsFromDeadEdges(current: GraphDirectedEdge, currentChain: MutableList<GraphDirectedEdge>, currentSquashedChain: MutableList<SquashedGraphEdge>) {
        // cut off old node form different branch
        val currentSourceNode = current.next(direction.flip())

        val matchingEdge = currentChain.firstOrNull { it.next() == currentSourceNode }
        val shortenedChain = currentChain.dropLastWhile { it != matchingEdge }
        val cleanupNeeded = shortenedChain.size != currentChain.size

        if (cleanupNeeded) {
            currentChain.retainAll(shortenedChain)
            currentSquashedChain.clear() // an edge an populate 2+ nodes so cleanup by removing is not easy
            currentChain.forEach { currentSquashedChain.addSquashed(it, filter, direction) }
        }

        currentChain.add(current)
        currentSquashedChain.addSquashed(current, filter, direction)
    }

    fun GraphDirectedEdge.next(navigationDirection: Direction = direction): GraphNode = when (navigationDirection) {
        Direction.Forward -> to
        Direction.Backward -> from
    }

    private fun ClassReference?.resolve(): AnalyzeClass? {
        if (this == null) return null
        return cache.classes[id()]
    }

    private fun MethodReference?.resolve(): AnalyzeMethod? {
        if (this == null) return null
        return cache.classes[classReference.id()]?.methods?.get(method)
    }

    private fun AnalyzeMethod.calls(): Sequence<GraphDirectedEdge> {
        if (edgeMode == EdgeMode.TypesOnly) return emptySequence()

        return if (direction == Direction.Forward) {
            cache.forwardCalls[id()]?.asReversed()?.asSequence()
                    ?.map { GraphDirectedEdge(it.source.resolve()!!, it.target.resolve()!!, it.toSingleList()) }
        } else {
            cache.backwardCalls[id()]?.asReversed()?.asSequence()
                    ?.map { GraphDirectedEdge(it.source.resolve()!!, it.target.resolve()!!, it.toSingleList()) }
        } ?: emptySequence()
    }

    private fun AnalyzeMethod.classUsages(): Sequence<GraphDirectedEdge> {
        if (edgeMode == EdgeMode.MethodsOnly) return emptySequence()

        return if (direction == Direction.Forward) {
            cache.forwardMethodClassUsage[id()]
                    ?.groupBy { it.clazz }?.asSequence()
                    ?.map { (clazz, references) -> GraphDirectedEdge(this, clazz, references) }
        } else {
            cache.forwardMethodClassUsage[id()]
                    ?.groupBy { it.clazz }?.asSequence()
                    ?.map { (clazz, references) -> GraphDirectedEdge(clazz, this, references) } // inversed for bi-directional
        } ?: emptySequence()
    }

    private fun AnalyzeClass.superTypeEdges(): Sequence<GraphDirectedEdge> {
        if (edgeMode == EdgeMode.MethodsOnly) return emptySequence()

        return if (direction == Direction.Forward) {
            superTypes.asSequence()
                    .mapNotNull { it.resolve() }
                    .map { GraphDirectedEdge(this, it, InheritanceType.Implementation.toSingleList()) }
        } else {
            superTypes.asSequence()
                    .mapNotNull { it.resolve() }
                    .map { GraphDirectedEdge(it, this, InheritanceType.Implementation.toSingleList()) }
        }
    }

    private fun AnalyzeClass.fieldEdges(): Sequence<GraphDirectedEdge> {
        if (edgeMode == EdgeMode.MethodsOnly) return emptySequence()

        return if (direction == Direction.Forward) {
            cache.forwardFieldClassUsage[reference]
                    ?.groupBy { it.target }?.asSequence()
                    ?.map { (target, fields) -> GraphDirectedEdge(this, target, fields) }
                    ?: emptySequence()
        } else {
            cache.backwardFieldClassUsage[reference]
                    ?.groupBy { it.field.containingClass.resolve()!! }?.asSequence()
                    ?.map { (source, fields) -> GraphDirectedEdge(source, this, fields) }
                    ?: emptySequence()
        }
    }

    private fun AnalyzeClass.subTypeEdges(): Sequence<GraphDirectedEdge> {
        if (edgeMode == EdgeMode.MethodsOnly) return emptySequence()

        return if (direction == Direction.Forward) {
            cache.impenitenceInverted[id()]?.asSequence()
                    ?.mapNotNull { it.resolve() }
                    ?.map { GraphDirectedEdge(this, it, InheritanceType.SubClass.toSingleList()) }
        } else {
            cache.impenitenceInverted[id()]?.asSequence()
                    ?.mapNotNull { it.resolve() }
                    ?.map { GraphDirectedEdge(it, this, InheritanceType.SubClass.toSingleList()) }

        } ?: sequenceOf()
    }

    private fun GraphNode.navigate(): Sequence<GraphDirectedEdge> {
        return when (this) {
            is AnalyzeMethod -> calls() + classUsages()
            is AnalyzeClass -> fieldEdges() + superTypeEdges() + subTypeEdges()
            else -> emptySequence()
        }.ensureUniqueDirectedEdge()
    }

    // non-unique / parallel edges destroy the graph traversal
    private fun Sequence<GraphDirectedEdge>.ensureUniqueDirectedEdge() = this
            .groupBy { "" + it.to.hashCode() + "_" + it.from.hashCode() }.values
            .map { edges ->
                val first = edges.first()
                if (edges.size > 1) {
                    GraphDirectedEdge(first.from, first.to, edges.flatMap { it.context })
                } else {
                    first
                }
            }.asSequence()
}

fun MutableList<SquashedGraphEdge>.addSquashed(edge: GraphDirectedEdge, filter: TraversalFilter, direction: Direction) {
    if (isEmpty() || last().isComplete()) {
        add(SquashedGraphEdge(edge, filter, direction))
    } else {
        last().push(edge, filter, direction)
    }
}

class SquashedGraphEdge(edge: GraphDirectedEdge, filter: TraversalFilter, val direction: Direction) {
    private val edges = mutableListOf<GraphDirectedEdge>()

    private val from: GraphNode
    private var to: GraphNode?

    init {
        from = edge.next(direction.flip())
        to = edge.next(direction).takeIf { filter.accept(it) }

        edges.add(edge)
    }

    fun edges() = if (direction == Direction.Forward) edges else edges.asReversed() // backward: edges are seen in the opposite order of the direction

    fun nodes() = listOfNotNull(from(), to())

    fun isComplete() = to != null

    fun push(edge: GraphDirectedEdge, filter: TraversalFilter, direction: Direction) {
        to = edge.next(direction).takeIf { filter.accept(it) }
        edges.add(edge)
    }

    fun from() = if (direction == Direction.Forward) from else to
    fun to() = if (direction == Direction.Forward) to else from

    fun GraphDirectedEdge.next(navigationDirection: Direction = direction): GraphNode = when (navigationDirection) {
        Direction.Forward -> to
        Direction.Backward -> from
    }

    override fun toString(): String {
        return if (direction == Direction.Forward) { // from/to is inverse for backward direction, but natural direction is printed
            "${from()}->${to()}"
        } else {
            "${to()}->${from()}"
        }
    }

    override fun equals(other: Any?): Boolean {
        return toString() == other?.toString()
    }

    override fun hashCode(): Int {
        return 31 * toString().hashCode()
    }


}

enum class Direction {
    Forward, Backward;

    fun flip() = when (this) {
        Forward -> Backward
        Backward -> Forward
    }
}

class SearchContext {
    var forwardDepth: Int? = null
    var backwardDepth: Int? = null
    var edgeMode: EdgeMode = EdgeMode.TypesAndMethods
    lateinit var roots: List<GraphNode>
}

private fun <T> List<T>.apply(direction: Direction): List<T> = when (direction) {
    Direction.Forward -> this
    Direction.Backward -> this.asReversed()
}
