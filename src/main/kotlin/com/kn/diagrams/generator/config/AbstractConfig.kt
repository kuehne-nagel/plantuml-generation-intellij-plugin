package com.kn.diagrams.generator.config

import com.intellij.psi.PsiClass
import com.kn.diagrams.generator.graph.GraphNode
import com.kn.diagrams.generator.graph.RestrictionFilter
import com.kn.diagrams.generator.graph.TraversalFilter

abstract class DiagramConfiguration(val rootClass: PsiClass) {

    abstract fun restrictionFilter(): RestrictionFilter

    abstract fun traversalFilter(rootNode: GraphNode): TraversalFilter

    companion object // use for serialization
}

fun String.attacheMetaData(config: DiagramConfiguration) = replace("@startuml", "@startuml\n\n" + config.metaDataSection() + "\n\n")

