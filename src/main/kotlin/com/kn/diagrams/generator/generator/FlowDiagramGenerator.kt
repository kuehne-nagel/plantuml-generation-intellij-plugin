package com.kn.diagrams.generator.generator

import com.intellij.psi.PsiMethod
import com.kn.diagrams.generator.builder.*
import com.kn.diagrams.generator.cast
import com.kn.diagrams.generator.config.FlowConfiguration
import com.kn.diagrams.generator.config.attacheMetaData
import com.kn.diagrams.generator.config.metaDataSection
import com.kn.diagrams.generator.graph.*
import com.kn.diagrams.generator.inReadAction
import com.kn.diagrams.generator.toSingleList

class FlowDiagramGenerator {
    fun createUmlContent(config: FlowConfiguration): List<Pair<String, String>> {
        val project = inReadAction { config.rootClass.project }
        val restrictionFilter = inReadAction { config.restrictionFilter() }
        val cache = GraphCache(project, restrictionFilter, config.projectClassification.searchMode)
        val diagram = DotDiagramBuilder()

        return config.perTerminalTaggedMethod { root ->
            val rootMethod = inReadAction { cache.methodFor(root)!! }
            val chains = cache.search(config.traversalFilter(rootMethod)) {
                roots = rootMethod.toSingleList()
                forwardDepth = config.graphTraversal.forwardDepth
                backwardDepth = config.graphTraversal.backwardDepth
                edgeMode = EdgeMode.MethodsOnly
            }
            val elementChains = processCallHierarchyToFlowChains(chains)
            val branches = createBranchesFromChains(elementChains)

            branches.forEach { (key, branch) ->
                var lastElement: FlowElement = branch.first()

                checkForGapsInConditionOrder(branch, diagram)

                key.conditionBranch()?.let { (condition, conditionBranch) ->
                    // connect the first branch element to the parent element
                    diagram.addLink(condition, lastElement.name) {
                        label = conditionBranch
                    }
                }

                branch.forEach { flowElement ->
                    diagram.addNode(flowElement)

                    if (isFirstElementAfterConditionBranches(lastElement, flowElement)) {
                        branches.lastActions(lastElement.relatedCondition)
                                .forEach { diagram.addLink(it, flowElement.name) }
                    } else if (lastElement.name != flowElement.name) {
                        diagram.addLink(lastElement.name, flowElement.name)
                    }

                    lastElement = flowElement
                }

            }

            diagram.create()
        }
    }

    private fun isFirstElementAfterConditionBranches(lastElement: FlowElement, flowElement: FlowElement) =
            lastElement.relatedCondition != flowElement.relatedCondition && !flowElement.isCondition()

    private fun createBranchesFromChains(elementChains: List<List<FlowElement>>): MutableMap<String, LinkedHashSet<FlowElement>> {
        val branches = mutableMapOf<String, LinkedHashSet<FlowElement>>()
        elementChains.forEach { es ->
            es.forEachIndexed { iChain, it ->
                val lastElement = if (iChain == 0) it else es[iChain - 1]

                if (it.isCondition()) {
                    branches.addConditionInParentBranch(it, lastElement)
                } else {
                    branches.addWithDowngrading(it)
                }
            }
        }
        return branches
    }

    private fun processCallHierarchyToFlowChains(chains: Set<List<SquashedGraphEdge>>): List<List<FlowElement>> {
        var lastBranch: String? = null
        var lastCondition: String? = null

        return chains.map { chain ->
            chain
                    .flatMap { listOfNotNull(it.from().cast<AnalyzeMethod>(), it.to().cast<AnalyzeMethod>()) }
                    .distinct()
                    .mapIndexed { i, method ->
                        if (i == 0) {
                            lastBranch = null
                            lastCondition = null
                        }

                        method.annotations
                                .filter { a -> relevant.any { it.annotationName == a.type.name } }
                                .map {
                                    val branch = it.parameter.firstOrNull { it.name == "branch" }?.value
                                    val alternativeBranch = it.parameter.firstOrNull { it.name == "alternativeBranch" }?.value
                                    val name = it.parameter.firstOrNull { it.name == "value" }?.value
                                            ?: method.cast<AnalyzeMethod>()!!.name
                                    if (branch != null && it.isCondition()) {
                                        lastCondition = name
                                        lastBranch = branch
                                    }
                                    FlowElement(it.type.name, name, lastCondition, lastBranch, alternativeBranch)
                                }
                    }.flatten()
        }.distinct()
    }

    private fun FlowConfiguration.perTerminalTaggedMethod(creator: (PsiMethod) -> String): List<Pair<String, String>> {
        val requestedMethod = rootMethod
        return rootClass.methods
                .filter { requestedMethod == null || requestedMethod == it }
                .filter { inReadAction { it.annotationsMapped().any { a -> relevant.any { it.annotationName == "FlowDiagramTerminal" } } } }
                .map { rootMethod ->
                    this.rootMethod = rootMethod
                    val plainDiagram = creator(rootMethod)
                    val diagramText = plainDiagram.attacheMetaData(this)

                    "${ inReadAction { rootMethod.name } }_flow" to diagramText
                }
    }

    private fun checkForGapsInConditionOrder(value: LinkedHashSet<FlowElement>, diagram: DotDiagramBuilder) {
        value.map { it.name to value.indexOf(it) }.groupBy { it.first }
                .mapValues { it.value.map { it.second } }
                .mapValues { it.value.maxOrNull()!! - it.value.minOrNull()!! - it.value.size + 1 }
                .filter { (_, gabs) -> gabs > 0 }
                .forEach { (incorrectCondition, _) ->
                    diagram.edges.add(DotLink(incorrectCondition, "incorrectOrder"))
                }
    }
}


data class FlowElement(val type: String, val name: String, var relatedCondition: String? = null, var relatedBranch: String? = null, val alternativeBranch: String? = null) {

    fun id() = if (relatedCondition != null) "$relatedCondition###$relatedBranch" else ""

    fun isCondition() = type == FlowElements.Condition.annotationName
    fun isAction() = type == FlowElements.Action.annotationName

    private fun key(): String {
        return if (isCondition()) {
            "$name;$relatedCondition;$relatedBranch"
        } else {
            name
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FlowElement) return false

        if (key() != other.key()) return false

        return true
    }

    override fun hashCode(): Int {
        return key().hashCode()
    }


}


fun AnalyzeAnnotation.isCondition() = type.name == FlowElements.Condition.annotationName

fun DotDiagramBuilder.addNode(element: FlowElement) {
    nodes.add(DotShape(element.name).with {
        when (element.type) {
            FlowElements.Action.annotationName -> shape = Rectangle
            FlowElements.Condition.annotationName -> shape = Diamond
            FlowElements.Terminal.annotationName -> shape = Oval
        }
    })
}

fun String.conditionBranch() = split("###").takeIf { it.size == 2 }?.let { it[0] to it[1] }

/**
 * condition elevation (only the first
 * main branch: action, condition (con
 * condition branchA: action, action
 * condition branchB: action, action
 */
private fun MutableMap<String, LinkedHashSet<FlowElement>>.addConditionInParentBranch(element: FlowElement, lastBranchesLastElement: FlowElement) {
    if (element.isCondition()) {
        correctGroup(element, lastBranchesLastElement)?.second?.add(element)
        // condition + branch may not be overwritten!!
    } else throw RuntimeException("${element.type} passed but Condition expected")
}

/**
 * main branch: action, condition (condition branch, alternative branch), *nothing will come here anymore*
 * alternative branch: action, action (from main branch), action (from main branch)
 */
private fun MutableMap<String, LinkedHashSet<FlowElement>>.addWithDowngrading(element: FlowElement) {
    correctGroup(element, element)?.let { (groupKey, group) ->
        groupKey.conditionBranch()?.let { (condition, branch) ->
            element.relatedCondition = condition
            element.relatedBranch = branch
        }
        group.add(element)
    }
}


private fun MutableMap<String, LinkedHashSet<FlowElement>>.correctGroup(element: FlowElement, groupElement: FlowElement): Pair<String, LinkedHashSet<FlowElement>>? {
    var groupKey = groupElement.id()
    var group = computeIfAbsent(groupKey) { LinkedHashSet() }
    while (true) {
        val lastOfGroup = group.lastOrNull()
        when {
            group.contains(element) -> return null // chains can have the same path; twice adding not needed
            lastOfGroup?.alternativeBranch != null -> { // downgrade needed?
                groupKey = lastOfGroup.relatedCondition + "###" + lastOfGroup.alternativeBranch
                group = computeIfAbsent(groupKey) { LinkedHashSet() }
            }
            else -> return groupKey to group
        }
    }
}

private fun MutableMap<String, LinkedHashSet<FlowElement>>.lastActions(lastElement: String?): Sequence<String> {
    val processed = mutableSetOf<FlowElement>()
    val allLast = mutableSetOf<String>()
    val queue = mutableListOf(lastElement)
    while (queue.isNotEmpty()) {
        val current = queue.removeAt(queue.size - 1) ?: continue

        asSequence()
                .filter { it.key.startsWith("$current##") }
                .forEach {
                    val last = it.value.last()
                    if (!processed.contains(last)) {
                        processed.add(last)
                        when {
                            last.isAction() -> allLast.add(last.name)
                            last.isCondition() -> queue.add(last.name)
                        }
                    }
                }
    }

    return allLast.asSequence()
}

enum class FlowElements(val annotationName: String) {
    Action("FlowDiagramAction"),
    Condition("FlowDiagramCondition"),
    Terminal("FlowDiagramTerminal"),
    Stop("FlowDiagramStop")
}

val relevant = FlowElements.values().asSequence().filter { it != FlowElements.Stop }
