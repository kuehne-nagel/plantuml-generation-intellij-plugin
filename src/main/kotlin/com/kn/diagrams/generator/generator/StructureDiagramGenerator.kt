package com.kn.diagrams.generator.generator

import com.kn.diagrams.generator.builder.DiagramDirection
import com.kn.diagrams.generator.builder.DotDiagramBuilder
import com.kn.diagrams.generator.config.StructureConfiguration
import com.kn.diagrams.generator.config.attacheMetaData
import com.kn.diagrams.generator.graph.EdgeMode
import com.kn.diagrams.generator.graph.GraphCache
import com.kn.diagrams.generator.graph.reference
import com.kn.diagrams.generator.inReadAction
import com.kn.diagrams.generator.toSingleList


class StructureDiagramGenerator {

    fun createUmlContent(config: StructureConfiguration): List<Pair<String, String>> {
        val project = inReadAction { config.rootClass.project }
        val restrictionFilter = inReadAction { config.restrictionFilter() }
        val cache = GraphCache(project, restrictionFilter, config.projectClassification.searchMode)

        val root = inReadAction { cache.classFor(config.rootClass)!! }

        val edges = cache.search(config.traversalFilter(root)) {
            roots = root.toSingleList()
            forwardDepth = config.graphTraversal.forwardDepth
            backwardDepth = config.graphTraversal.backwardDepth
            edgeMode = EdgeMode.TypesOnly
        }.flatten()

        val visualizationConfiguration = inReadAction { config.visualizationConfig(cache) }
        val dot = DotDiagramBuilder()
        dot.direction = DiagramDirection.TopToBottom

        when (config.details.aggregation) {
            Aggregation.ByClass -> dot.aggregateByClass(edges, visualizationConfiguration)
            Aggregation.GroupByClass -> dot.groupByClass(edges, visualizationConfiguration)
            Aggregation.None -> dot.noAggregation(edges, visualizationConfiguration)
        }

        return listOf("structure" to dot.create().attacheMetaData(config))
    }
}

fun StructureConfiguration.visualizationConfig(cache: GraphCache) = DiagramVisualizationConfiguration(
    cache.classes[rootClass.reference().id()]!!,
    projectClassification,
    projectClassification.includedProjects,
    projectClassification.pathEndKeywords,
    details.showPackageLevels,
    details.showClassGenericTypes,
    details.showMethods,
    details.showMethodParameterTypes,
    details.showMethodParameterNames,
    details.showMethodReturnType,
    false,
    details.showDetailedClassStructure
)
