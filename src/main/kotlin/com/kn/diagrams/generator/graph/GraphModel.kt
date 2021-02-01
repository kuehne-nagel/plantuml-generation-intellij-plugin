package com.kn.diagrams.generator.graph

interface GraphNode

interface EdgeContext

class FieldWithTargetType(val field: AnalyzeField, val target: AnalyzeClass) : EdgeContext
data class MethodClassUsage(val clazz: AnalyzeClass, val method: AnalyzeMethod, val reference: String) : EdgeContext
enum class InheritanceType : EdgeContext { Implementation, SubClass }

class GraphDirectedEdge(val from: GraphNode, val to: GraphNode, val context: List<EdgeContext>) { // also no parallel edges

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GraphDirectedEdge) return false

        if (from != other.from) return false
        if (to != other.to) return false

        return true
    }

    override fun hashCode(): Int {
        var result = from.hashCode()
        result = 31 * result + to.hashCode()
        return result
    }

    override fun toString(): String {
        return "$from->$to"
    }

}
