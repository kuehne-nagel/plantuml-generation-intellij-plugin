package com.kn.diagrams.generator.generator

import com.intellij.util.castSafelyTo
import com.kn.diagrams.generator.builder.*
import com.kn.diagrams.generator.escapeHTML
import com.kn.diagrams.generator.graph.*
import com.kn.diagrams.generator.notReachable
import java.awt.Color
import kotlin.math.absoluteValue


data class DiagramVisualizationConfiguration(
    val rootNode: GraphNode,
    val projectClassification: ProjectClassification,
    val pathStartKeywords: String,
    val pathEndKeywords: String,
    val showPackageLevels: Int,
    val showClassGenericTypes: Boolean,
    val showClassMethods: Boolean,
    val showMethodParametersTypes: Boolean,
    val showMethodParametersNames: Boolean,
    val showMethodReturnType: Boolean,
    val showCallOrder: Boolean,
    val showDetailedClass: Boolean
)

fun DotDiagramBuilder.addDirectLink(edge: SquashedGraphEdge, config: DiagramVisualizationConfiguration) {
    val hasMoreEdges = edge.edges().size > 1
    val relevantEdge = if (edge.direction == Direction.Forward) edge.edges().last() else edge.edges().first()

    relevantEdge.perContext { context ->
        val (from, to) = fromAndTo(context, edge)

        if (from == to && edge.edges().flatMap { it.context }.any { it is InheritanceType }) {
            return@perContext
        }

        addLink(from, to) {
            when (context) {
                is MethodClassUsage -> {
                    style = "dashed"
                    if (!hasMoreEdges) {
                        label = context.reference
                    }
                }
                is AnalyzeCall -> {
                    label = context.sequence.toString().takeIf { it != "-1" }.takeIf { config.showCallOrder }
                }
                is FieldWithTargetType -> {
                    label = context.field.name + "\n" + context.field.cardinality()
                }
                is InheritanceType -> {
                    dir = "both"
                    arrowHead = "none"
                    arrowTail = "empty"
                }
            }
        }
    }
}

private fun fromAndTo(context: EdgeContext, edge: SquashedGraphEdge): Pair<String, String> {
    return when (context) { // fixing from/to for forced directed bi-directional edges
        is MethodClassUsage -> edge.direction.switchIfBackward(edge.diagramIdsFromTo())
        InheritanceType.SubClass -> edge.direction.switchIfBackward(edge.diagramIdsFromTo())
        InheritanceType.Implementation -> edge.direction.switchIfBackward(edge.diagramIdsToFrom())
        else -> edge.diagramIdsFromTo()
    }
}

private fun Direction.switchIfBackward(fromTo: Pair<String, String>): Pair<String, String> {
    return if (this == Direction.Forward) {
        fromTo
    } else {
        fromTo.second to fromTo.first
    }
}

private fun SquashedGraphEdge.diagramIdsFromTo() =
    from()!!.diagramId() to to()!!.diagramId()

private fun SquashedGraphEdge.diagramIdsToFrom() =
    to()!!.diagramId() to from()!!.diagramId()


fun ClassReference.diagramId() = name + path.hashCode().absoluteValue

fun GraphNode.diagramId() = when (this) {
    is AnalyzeMethod -> containingClass.diagramId() + "XXX" + name + parameter.joinToString { it.typeDisplay }.hashCode().absoluteValue
    is AnalyzeClass -> reference.diagramId()
    else -> notReachable()
}

fun GraphNode.containingClass() = when (this) {
    is AnalyzeClass -> reference
    is AnalyzeMethod -> containingClass
    else -> notReachable()
}

fun MethodVisibility.color() = when (this) {
    MethodVisibility.PRIVATE -> "red"
    MethodVisibility.PROTECTED -> "blue"
    MethodVisibility.PACKAGE_LOCAL -> "blue"
    MethodVisibility.PUBLIC -> "darkgreen"
}

fun AnalyzeClass.color() = when (reference.classType) {
    ClassType.Interface -> "#F1E5FD"
    ClassType.Enum -> "#DCFBD5"
    else -> "#FFFFFF"
}

fun AnalyzeClass.symbol() = when (reference.classType) {
    ClassType.Interface -> "(I)"
    ClassType.Enum -> "(E)"
    ClassType.Class -> "(C)"
}

fun MethodVisibility.symbol() = when (this) {
    MethodVisibility.PRIVATE -> "- "
    MethodVisibility.PROTECTED -> "# "
    MethodVisibility.PUBLIC -> "+ "
    MethodVisibility.PACKAGE_LOCAL -> "# "
}

fun AnalyzeMethod.signature(config: DiagramVisualizationConfiguration? = null): String {
    val showTypes = config?.showMethodParametersTypes != false
    val showNames = config?.showMethodParametersNames != false
    val parameters = if (showTypes || showNames) {
        parameter.joinToString(", ") { listOfNotNull(it.name.takeIf { showNames }, it.typeDisplay.takeIf { showTypes }).joinToString(": ") }
    } else ""

    val returnType = if (config?.showMethodReturnType != false && returnTypeDisplay != null && returnTypeDisplay != "void") {
        ": $returnTypeDisplay"
    } else ""

    return "${visibility.symbol() + name}($parameters)$returnType"
}

fun ClassReference.diagramNameWithId() = name + hashCode().absoluteValue.toString()

val mandatoryAnnotations = sequenceOf("NotNull", "NotBlank", "NotEmpty")
private fun Variable.hasMandatoryAnnotation(): Boolean {
    return annotations.any {
        mandatoryAnnotations.contains(it.type.name)
                || (it.type.name == "XmlElement" && it.attribute("required")?.value == "true")
                || (it.type.name == "Autowired" && it.attribute("required")?.value != "false")
    }
}

fun Variable?.cardinality(): String {
    if (this == null) return ""

    val isMandatory = isPrimitive
            || castSafelyTo<AnalyzeField>()?.isEnumInstance == true
            || hasMandatoryAnnotation()

    return when {
        isCollection -> {
            val minimum = listOfNotNull(
                    annotations.withName("Min")?.attribute("value")?.value,
                    annotations.withName("Size")?.attribute("min")?.value,
                    "0"
            ).first()

            val maximum = listOfNotNull(
                    annotations.withName("Max")?.attribute("value")?.value,
                    annotations.withName("Size")?.attribute("max")?.value?.takeIf { it != "2147483647" },
                    "*"
            ).first()

            "[$minimum..$maximum]"
        }
        else -> when {
            isMandatory -> "[1]"
            else -> "[0..1]"
        }
    }
}

fun AnalyzeClass.createBoxOrTableShape(config: DiagramVisualizationConfiguration) = if (config.showDetailedClass) {
    createHTMLShape(config)
} else {
    createBoxShape()
}

fun AnalyzeClass.createBoxShape() = DotShape(symbol() + reference.name, diagramId()).with {
    shape = Rectangle
    style = "filled"
    fillColor = color()
}

fun AnalyzeClass.createHTMLShape(config: DiagramVisualizationConfiguration) = DotHTMLShape(reference.name, diagramId()).with {
    this.config.style = "filled"
    this.config.fillColor = color()
    this.config.margin = 0

    withTable {
        cellBorder = 0
        cellPadding = 4

        row {
            cell(symbol() + reference.displayName)
        }

        if (fields.isNotEmpty()) horizontalSeparator()
        fields.sortedWith(compareBy({ !it.isEnumInstance }, { it.name }))
            .forEach { field ->
                row {
                    cell(field.visibility.symbol() + "  " + field.name + ": " + field.typeDisplay + " " + field.cardinality())
                }
            }

        if (classType == ClassType.Interface || config.showClassMethods) {
            if (methods.isNotEmpty()) horizontalSeparator()
            methods.values.sortedBy { it.name }.forEach { method ->
                row {
                    cell(method.signature(config))
                }
            }
        }

    }
}


fun Color.toHex() = String.format("%02x%02x%02x", red, green, blue)

val groupColorLevel = listOf(
    Color(236, 236, 236),
    Color(216, 216, 216),
    Color(196, 196, 196),
    Color(186, 186, 186),
    Color(176, 176, 176),
    Color(166, 166, 166),
    Color(156, 156, 156),
    Color(146, 146, 146),
    Color(136, 136, 136)
)

fun GraphDirectedEdge.perContext(mapping: GraphDirectedEdge.(EdgeContext) -> Unit) {
    context.forEach {
        mapping(this, it)
    }
}
