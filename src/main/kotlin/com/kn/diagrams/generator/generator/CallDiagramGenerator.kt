package com.kn.diagrams.generator.generator

import com.intellij.psi.PsiMethod
import com.kn.diagrams.generator.builder.DiagramDirection
import com.kn.diagrams.generator.builder.DotDiagramBuilder
import com.kn.diagrams.generator.config.CallConfiguration
import com.kn.diagrams.generator.config.attacheMetaData
import com.kn.diagrams.generator.graph.GraphCache
import com.kn.diagrams.generator.graph.isPrivate
import com.kn.diagrams.generator.graph.reference
import com.kn.diagrams.generator.inReadAction
import com.kn.diagrams.generator.notifications.notifyErrorMissingPublicMethod
import com.kn.diagrams.generator.toSingleList


class CallDiagramGenerator {

    fun createUmlContent(config: CallConfiguration): List<Pair<String, String>> {
        return config.perPublicMethod { rootMethod ->
            val project = inReadAction { config.rootClass.project }
            val restrictionFilter = inReadAction { config.restrictionFilter() }

            val cache = GraphCache(project, restrictionFilter, config.projectClassification.searchMode)
            val root = inReadAction { cache.methodFor(rootMethod)!! }

            val edges = cache.search(config.traversalFilter(root)) {
                roots = root.toSingleList()
                forwardDepth = config.graphTraversal.forwardDepth
                backwardDepth = config.graphTraversal.backwardDepth
                edgeMode = config.details.edgeMode
            }.flatten()

            val dot = DotDiagramBuilder()
            val visualizationConfig = inReadAction { config.visualizationConfig(cache) }
            dot.direction = DiagramDirection.LeftToRight

            when (config.details.aggregation) {
                Aggregation.ByClass -> dot.aggregateByClass(edges, visualizationConfig)
                Aggregation.GroupByClass -> dot.groupByClass(edges, visualizationConfig)
                Aggregation.None -> dot.noAggregation(edges, visualizationConfig)
            }

            dot.create()
        }
    }
}



private fun CallConfiguration.perPublicMethod(creator: (PsiMethod) -> String): List<Pair<String, String>> {
    val requestedMethod = rootMethod

    val diagramsPerMethod =
        inReadAction {
            rootClass.methods
                .filter { requestedMethod == null || requestedMethod == it }
                .filter { requestedMethod != null || !it.isPrivate() }
        }.mapIndexed { i, rootMethod ->
                this.rootMethod = rootMethod
                val plainDiagram = creator(rootMethod)
                val diagramText = plainDiagram.attacheMetaData(this)

                "${i}_${inReadAction { rootMethod.name }}_calls" to diagramText
            }

    if (diagramsPerMethod.isEmpty()) {
        notifyErrorMissingPublicMethod(inReadAction { rootClass.project }, rootClass, rootMethod)
    }

    return diagramsPerMethod
}


fun CallConfiguration.visualizationConfig(cache: GraphCache) = DiagramVisualizationConfiguration(
    rootMethod?.let { cache.methodFor(it) } ?: cache.classes[rootClass.reference().id()]!!,
    projectClassification,
    projectClassification.includedProjects,
    projectClassification.pathEndKeywords,
    details.showPackageLevels,
    false,
    false,
    details.showMethodParametersTypes,
    details.showMethodParametersNames,
    details.showMethodReturnType,
    details.showCallOrder,
    details.showDetailedClassStructure
)


