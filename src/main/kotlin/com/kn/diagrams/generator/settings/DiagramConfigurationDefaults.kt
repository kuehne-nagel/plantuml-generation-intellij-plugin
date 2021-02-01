package com.kn.diagrams.generator.settings

import com.kn.diagrams.generator.config.CallDiagramDetails
import com.kn.diagrams.generator.config.StructureDiagramDetails
import com.kn.diagrams.generator.config.serializer
import com.kn.diagrams.generator.generator.Aggregation
import com.kn.diagrams.generator.graph.EdgeMode
import com.kn.diagrams.generator.graph.GraphRestriction
import com.kn.diagrams.generator.graph.GraphTraversal
import com.kn.diagrams.generator.graph.ProjectClassification


class ConfigurationDefaults {
    companion object {
        fun classification(): ProjectClassification = serializer.fromJson(DiagramGenerationSettings.instance.projectClassification, ProjectClassification::class.java)
        fun callDiagram(): CallConfigurationDefaults = serializer.fromJson(DiagramGenerationSettings.instance.callDiagramDefaults, CallConfigurationDefaults::class.java)
        fun structureDiagram(): StructureConfigurationDefaults = serializer.fromJson(DiagramGenerationSettings.instance.structureDiagramDefaults, StructureConfigurationDefaults::class.java)
        fun flowDiagram(): FlowConfigurationDefaults = serializer.fromJson(DiagramGenerationSettings.instance.flowDiagramDefaults, FlowConfigurationDefaults::class.java)
    }
}

class CallConfigurationDefaults(
        var graphRestriction: GraphRestriction = GraphRestriction(),
        var graphTraversal: GraphTraversal = GraphTraversal(),
        var details: CallDiagramDetails = CallDiagramDetails()) {

    fun defaulted(): CallConfigurationDefaults {
        with(graphRestriction) {
            cutDataAccess = true
            cutDataStructures = true
            cutInterfaceStructures = true
            cutConstructors = true
            cutGetterAndSetter = true
            cutEnum = true
            cutMappings = false
        }
        with(graphTraversal) {
            backwardDepth = 3
            forwardDepth = 3
            hideDataStructures = false
            hideInterfaceCalls = true
            hideMappings = false
            hidePrivateMethods = true
            onlyShowApplicationEntryPoints = false
        }
        with(details) {
            aggregation = Aggregation.GroupByClass
            showMethodParametersTypes = false
            showMethodParametersNames = false
            showMethodReturnType = false
            showPackageLevels = 2
            showCallOrder = false
            showDetailedClassStructure = false
            edgeMode = EdgeMode.MethodsOnly
        }
        return this
    }
}

class StructureConfigurationDefaults(
        var graphRestriction: GraphRestriction = GraphRestriction(),
        var graphTraversal: GraphTraversal = GraphTraversal(),
        var details: StructureDiagramDetails = StructureDiagramDetails()) {

    fun defaulted(): StructureConfigurationDefaults {
        with(graphRestriction) {
            cutConstructors = false
            cutDataAccess = false
            cutDataStructures = false
            cutInterfaceStructures = false
            cutGetterAndSetter = true
            cutConstructors = true
            cutEnum = true
            cutMappings = false
        }
        with(graphTraversal) {
            backwardDepth = 6
            forwardDepth = 6
            hideDataStructures = false
            hideInterfaceCalls = true
            hideMappings = false
            hidePrivateMethods = true
            onlyShowApplicationEntryPoints = false
        }
        with(details) {
            aggregation = Aggregation.GroupByClass
            showMethodParameterNames = true
            showMethodParameterTypes = true
            showMethodReturnType = true
            showPackageLevels = 2
            showDetailedClassStructure = true
        }
        return this
    }
}

class FlowConfigurationDefaults(
        var graphRestriction: GraphRestriction = GraphRestriction(),
        var graphTraversal: GraphTraversal = GraphTraversal()) {

    fun defaulted(): FlowConfigurationDefaults {
        with(graphRestriction) {
            cutConstructors = false
            cutDataAccess = false
            cutDataStructures = false
            cutInterfaceStructures = false
            cutGetterAndSetter = false
            cutEnum = false
            cutMappings = false
        }
        with(graphTraversal) {
            backwardDepth = 0
            forwardDepth = 999
            hideDataStructures = false
            hideInterfaceCalls = false
            hideMappings = false
            hidePrivateMethods = false
            onlyShowApplicationEntryPoints = false
        }
        return this
    }
}
