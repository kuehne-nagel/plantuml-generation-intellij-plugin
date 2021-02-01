package com.kn.diagrams.generator.builder

import com.kn.diagrams.generator.cast
import com.kn.diagrams.generator.escapeHTML
import kotlin.math.abs
import kotlin.reflect.KMutableProperty0

abstract class DotNode {
    abstract fun id(): String
    abstract fun create(): String
}

class DotShapeConfig(var label: String? = null,
                     var fillColor: String? = null,
                     var shape: DotShapeConstant? = null,
                     var style: String? = null,
                     var margin: Int? = null,
                     var tooltip: String? = null,
                     var penWidth: Int? = null,
                     var fontColor: String? = null) {
    fun create(): String {
        return formattedConfigString(ConfigLineFormat.MultiLine,
                ::label, ::style, ::margin, ::shape, ::fillColor, ::tooltip, ::penWidth, ::fontColor)
    }
}

class DotHTMLCell(val text: String, var port: String? = null) {
    fun create(): String {
        return "<TD ALIGN=\"LEFT\" ${port?.let { "PORT=\"$it\"" } ?: ""}>${text.escapeHTML()}</TD>"
    }
}

class DotHTMLHorizontalSeparatorRow : DotHTMLTableRowElement {
    override fun create() = "<HR/>"
}

class DotHTMLRow : DotHTMLTableRowElement {
    val cells = mutableListOf<DotHTMLCell>()

    fun cell(text: String, actions: (DotHTMLCell.() -> Unit)? = null) {
        val cell = DotHTMLCell(text)
        actions?.let { it(cell) }
        cells.add(cell)
    }

    override fun create(): String {
        return "<TR>${cells.joinToString("") { it.create() }}</TR>"
    }
}

interface DotHTMLTableRowElement {
    fun create(): String
}

class DotHTMLTable(var border: Int = 1, var cellBorder: Int = 1, var cellSpacing: Int = 0, var cellPadding: Int = 2) {
    private val rows = mutableListOf<DotHTMLTableRowElement>()

    fun with(actions: DotHTMLTable.() -> Unit): DotHTMLTable {
        actions(this)
        return this
    }

    fun row(actions: DotHTMLRow.() -> Unit) {
        val row = DotHTMLRow()
        actions(row)
        rows.add(row)
    }

    fun horizontalSeparator() {
        rows.add(DotHTMLHorizontalSeparatorRow())
    }

    fun create(): String {
        return """
            |<<TABLE BORDER="$border" CELLBORDER="$cellBorder" CELLPADDING="$cellPadding" CELLSPACING="$cellSpacing">
            |${rows.joinToString("\n") { it.create() }}
            |</TABLE>>
        """.trimMargin("|")
    }
}

fun DotDiagramBuilder.addHTMLShape(name: String, id: String = name, configure: DotHTMLShape.() -> Unit) {
    val shape = DotHTMLShape(name, id)
    configure(shape)
    nodes.add(shape)
}

class DotHTMLShape(val name: String, val id: String = name) : DotNode() {
    val config = DotShapeConfig(shape = "plaintext")
    val table = DotHTMLTable()

    override fun id(): String {
        return id.formatId()
    }

    override fun create(): String {
        config.label = table.create()
        return "${id()}${config.create()}"
    }

    fun with(actions: DotHTMLShape.() -> Unit): DotHTMLShape {
        actions(this)
        return this
    }

    fun withTable(actions: DotHTMLTable.() -> Unit): DotHTMLShape {
        actions(table)
        return this
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DotHTMLShape

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

}

fun DotDiagramBuilder.addShape(name: String, id: String = name, configure: (DotShapeConfig.() -> Unit)? = null) {
    val shape = DotShape(name, id)
    configure?.let { it(shape.config) }
    nodes.add(shape)
}

fun DotDiagramBuilder.addCluster(name: String, id: String = name, configure: (DotClusterConfig.() -> Unit)? = null): DotCluster {
    val shape = DotCluster(name, id)
    configure?.let { it(shape.config) }
    nodes.add(shape)

    return shape
}

class DotShape(name: String, val id: String = name) : DotNode() {
    val config: DotShapeConfig = DotShapeConfig(label = name)

    override fun id(): String {
        return id.formatId()
    }

    override fun create(): String {
        return "${id()}${config.create()}"
    }

    fun with(actions: DotShapeConfig.() -> Unit): DotShape {
        actions(this.config)
        return this
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DotShape

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }


}

private fun containsInvalidCharacters(str: String) = !str.startsWith("<<")
        && !"[0-9a-zA-Z_]+".toRegex().matches(str)


class DotClusterConfig(var label: String? = null, var labelJust: String = "l", var fillColor: String? = null, var style: String? = null) {
    fun create(): String {
        return formattedConfigString(ConfigLineFormat.ClusterLine, ::label, ::labelJust, ::fillColor, ::style)
    }
}

open class DotCluster(val name: String, val id: String = name) : DotNode() {
    val config: DotClusterConfig = DotClusterConfig(label = name)
    val childs: MutableSet<DotNode> = mutableSetOf()

    override fun id(): String {
        return "cluster_${abs(id.hashCode())}"
    }

    override fun create(): String {
        return """
            |subgraph ${id()} { 
            |   ${config.create()}
            |   
            |   ${childs.map { it.create() }.sorted().joinToString("\n\n")}
            |} 
        """.trimMargin("|")
    }

    fun with(actions: DotCluster.() -> Unit): DotCluster {
        actions(this)
        return this
    }


    fun addShape(name: String, id: String = name, configure: (DotShapeConfig.() -> Unit)? = null) {
        val shape = DotShape(name, id)
        configure?.let { it(shape.config) }
        childs.add(shape)
    }

    fun addHTMLShape(name: String, id: String = name, configure: (DotHTMLShape.() -> Unit)? = null) {
        val shape = DotHTMLShape(name, id)
        configure?.let { it(shape) }
        childs.add(shape)
    }

    fun addCluster(name: String, id: String = name, configure: (DotClusterConfig.() -> Unit)? = null): DotCluster {
        val cluster = DotCluster(name, id)
        configure?.let { it(cluster.config) }
        childs.add(cluster)

        return cluster
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DotCluster

        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }


}

abstract class DotEdge {
    abstract fun create(): String
}

fun DotDiagramBuilder.addLink(fromId: String, toId: String, configure: (DotLinkConfig.() -> Unit)? = null) {
    val link = DotLink(fromId, toId)
    configure?.let { it(link.config) }
    edges.add(link)
}

class DotLink(val fromId: String, val toId: String) : DotEdge() {
    val config: DotLinkConfig = DotLinkConfig()

    override fun create(): String {
        return "${fromId.formatId()} -> ${toId.formatId()}${config.create()};"
    }

    fun with(actions: DotLinkConfig.() -> Unit): DotLink {
        actions(config)
        return this
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DotLink) return false

        if (fromId != other.fromId) return false
        if (toId != other.toId) return false
        if (config != other.config) return false

        return true
    }

    override fun hashCode(): Int {
        var result = fromId.hashCode()
        result = 31 * result + toId.hashCode()
        result = 31 * result + config.hashCode()
        return result
    }


}

data class DotLinkConfig(var label: String? = null, var tooltip: String? = null, var style: String? = null, var arrowHead: String? = null, var arrowTail: String? = null, var weight: Int? = null, var dir: String? = null, var headPort: String? = null, var tailPort: String? = null) {
    fun create(): String {
        return formattedConfigString(ConfigLineFormat.SingleLine, ::label, ::tooltip, ::style, ::arrowHead, ::arrowTail, ::weight, ::dir, ::headPort, ::tailPort)
    }
}

enum class ConfigLineFormat(val lineSeparator: String) {
    SingleLine(", "),
    MultiLine("\n|\t"),
    ClusterLine("\n|\t")

}

fun formattedConfigString(lineType: ConfigLineFormat = ConfigLineFormat.SingleLine, vararg parameter: KMutableProperty0<*>): String {
    val values = parameter
            .map { it.name.toLowerCase() to it.get() }
            .filter { (_, value) -> value != null }
            .joinToString(lineType.lineSeparator) { (key, value) ->
                if (containsInvalidCharacters(value!!.toString())) {
                    """$key="$value""""
                } else {
                    """$key=$value"""
                }
            }

    if (values.isEmpty()) {
        return ""
    }
    return when (lineType) {
        ConfigLineFormat.SingleLine -> "[$values]"
        ConfigLineFormat.ClusterLine -> """
            |${"\t"}$values
        """.trimMargin("|")
        else -> """
            |[
            |${"\t"}$values
            |];
        """.trimMargin("|")
    }
}

class DotDiagramBuilder {
    var direction: DiagramDirection = DiagramDirection.TopToBottom
    var layout: String? = null
    var nodes: MutableSet<DotNode> = mutableSetOf()
    var edges: MutableSet<DotEdge> = mutableSetOf()

    fun create(): String {
        return """
            |@startuml
            |
            |digraph g {
            |    rankdir="${direction.rankDir}"
            |    splines=polyline
            |    ${layout?.let { "layout=$it\n|\toverlap=false" } ?: ""}
            |
            |'nodes 
            |${nodes.asSequence().map { it.create() }.sorted().joinToString("\n\n")}
            |
            |'edges    
            |${edges.asSequence().map { it.create() }.sorted().joinToString("\n")}
            |    
            |}
            |@enduml
        """.trimMargin("|")
    }
}

enum class DiagramDirection(val rankDir: String) {
    LeftToRight("LR"), TopToBottom("TB")
}

private fun String.formatId() = this.replace(" ", "")


typealias DotShapeConstant = String

val Rectangle = "rect".cast<DotShapeConstant>()
val Oval = "oval".cast<DotShapeConstant>()
val Diamond = "diamond".cast<DotShapeConstant>()
