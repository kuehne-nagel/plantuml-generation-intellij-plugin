package com.kn.diagrams.generator.generator

import com.kn.diagrams.generator.builder.DotDiagramBuilder
import com.kn.diagrams.generator.builder.Rectangle
import com.kn.diagrams.generator.builder.addLink
import com.kn.diagrams.generator.builder.addShape
import com.kn.diagrams.generator.graph.AnalyzeClass
import com.kn.diagrams.generator.graph.AnalyzeMethod
import com.kn.diagrams.generator.graph.ClassReference
import com.kn.diagrams.generator.graph.SquashedGraphEdge
import com.kn.diagrams.generator.notReachable


enum class Aggregation {
    ByClass, GroupByClass, None
}

fun DotDiagramBuilder.noAggregation(edges: List<SquashedGraphEdge>, config: DiagramVisualizationConfiguration) {
    fun addShapeForMethod(method: AnalyzeMethod) = addShape(method.signature(config), method.diagramId()) {
        penWidth = if (method == config.rootNode) 4 else null
        tooltip = method.containingClass.name + "\n\n" + method.javaDoc
        fontColor = method.visibility.color()
    }

    when (config.rootNode) {
        is AnalyzeMethod -> addShapeForMethod(config.rootNode)
        is AnalyzeClass -> nodes.add(config.rootNode.createBoxOrTableShape(config))
    }

    edges
        .forEach { edge ->
            edge.nodes().forEach { node ->
                when (node) {
                    is AnalyzeMethod -> addShapeForMethod(node)
                    is AnalyzeClass -> nodes.add(node.createBoxOrTableShape(config))
                    else -> notReachable()
                }
            }

            addDirectLink(edge, config)
        }
}


fun DotDiagramBuilder.aggregateByClass(edges: List<SquashedGraphEdge>, config: DiagramVisualizationConfiguration) {
    with(config.projectClassification) {
        fun addShapeClass(clazz: ClassReference) = addShape(clazz.name, clazz.diagramNameWithId()) {
            if (clazz.isDataStructure() || clazz.isInterfaceStructure()) {
                shape = Rectangle
            }
        }

        addShapeClass(config.rootNode.containingClass())

        edges
            .filter { it.from()!!.containingClass() != it.to()!!.containingClass() }
            .groupBy { it.from()!!.containingClass().id() + "_" + it.to()!!.containingClass().id() }
            .values
            .forEach { groupEdges ->
                val from = groupEdges.first().from()!!.containingClass()
                val to = groupEdges.first().to()!!.containingClass()

                sequenceOf(from, to).forEach { addShapeClass(it) }

                addLink(from.diagramNameWithId(), to.diagramNameWithId()) {
                    weight = groupEdges.distinct().size
                    label = "calls = " + groupEdges.distinct().size
                    if (from.isDataStructure() xor to.isDataStructure()) {
                        style = "dashed"
                    }
                }
            }
    }
}


