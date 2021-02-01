package com.kn.diagrams.generator.generator

import com.intellij.util.castSafelyTo
import com.kn.diagrams.generator.builder.DotCluster
import com.kn.diagrams.generator.builder.DotClusterConfig
import com.kn.diagrams.generator.builder.DotDiagramBuilder
import com.kn.diagrams.generator.builder.DotNode
import com.kn.diagrams.generator.graph.*


fun DotDiagramBuilder.groupByClass(edges: List<SquashedGraphEdge>, config: DiagramVisualizationConfiguration) {
    val classClusterCache = mutableMapOf<ClassReference, DotCluster>()

    val pathCluster = DotPathGroup("path cluster manager") { i, packageCluster ->
        packageCluster.config.style = "filled"
        packageCluster.config.fillColor = "#" + groupColorLevel[i].toHex()

    }
    nodes.add(pathCluster)

    fun addShapeForMethod(method: AnalyzeMethod) = classClusterCache.computeIfAbsent(method.containingClass()) {
        pathCluster.addGroup(it.name, it.qualifiedName(), it.diagramPath(config))
    }.with {
        this.config.style = "filled"
        this.config.fillColor = "white"
    }.addShape(method.signature(config), method.diagramId()) {
        penWidth = if (method == config.rootNode) 4 else null
        tooltip = method.containingClass.name + "\n\n" + method.javaDoc
        fontColor = method.visibility.color()
        style = "filled"
        fillColor = "white"
    }

    when (config.rootNode) {
        is AnalyzeMethod -> addShapeForMethod(config.rootNode)
        is AnalyzeClass -> pathCluster.addNode(config.rootNode.createBoxOrTableShape(config), config.rootNode.reference.diagramPath(config))
    }

    edges.forEach { edge ->
        edge.nodes().forEach { node ->
            when (node) {
                is AnalyzeMethod -> addShapeForMethod(node)
                is AnalyzeClass -> pathCluster.addNode(node.createBoxOrTableShape(config), node.reference.diagramPath(config))
            }
        }

        addDirectLink(edge, config)
    }
}


fun ClassReference.diagramPath(config: DiagramVisualizationConfiguration): String {
    var diagramPath = path
    config.pathStartKeywords.bySemicolon().forEach {
        diagramPath = diagramPath.substringAfter("$it.")
    }
    config.pathEndKeywords.takeUnless { it == "" }?.bySemicolon()?.forEach {
        diagramPath = diagramPath.substringBefore(".$it")
    }

    return diagramPath
        .split(".")
        .let { it.subList(0, Integer.max(0, Integer.min(it.size, config.showPackageLevels))) }
        .joinToString(".")
}

class DotPathGroup(name: String, id: String = name, val applyLayout: (Int, DotCluster) -> Unit) : DotCluster(name, id) {

    override fun create(): String {
        // this is only a fake cluster to avoid searching in the main graph and clusters
        return childs.map { it.create() }.sorted().joinToString("\n\n")
    }

    fun addGroup(name: String, id: String = name, path: String? = null, configure: (DotClusterConfig.() -> Unit)? = null): DotCluster {
        val cluster = DotCluster(name, id)
        configure?.let { it(cluster.config) }
        addNode(cluster, path)

        return cluster
    }

    fun addNode(node: DotNode, path: String? = null) {
        if (path == "" || path == null) {
            childs.add(node)
            return
        }

        var currentGroup: DotCluster = this
        val parts = path.split(".")
        parts.forEachIndexed { i, part ->
            val groupPath = parts.subList(0, i + 1).joinToString(".")
            val existingGroup = currentGroup.childs.firstOrNull { it.castSafelyTo<DotCluster>()?.id == groupPath }

            currentGroup = existingGroup as? DotCluster ?: currentGroup.addCluster(part, groupPath).with {
                applyLayout(i, this)
            }

        }

        currentGroup.childs.add(node)
    }
}

