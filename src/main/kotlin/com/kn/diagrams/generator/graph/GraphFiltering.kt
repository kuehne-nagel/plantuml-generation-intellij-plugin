package com.kn.diagrams.generator.graph

import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier
import com.kn.diagrams.generator.inCase
import com.kn.diagrams.generator.inReadAction
import com.kn.diagrams.generator.notReachable


interface TraversalFilter {
    fun accept(node: GraphNode): Boolean
}

interface RestrictionFilter {
    fun acceptClass(clazz: ClassReference): Boolean
    fun removeClass(clazz: ClassReference, cache: GraphCache): Boolean // postprocessing based on other classes
    fun acceptMethod(method: AnalyzeMethod): Boolean
}
val ClassReference.isEnum get() = classType == ClassType.Enum

class GraphRestrictionFilter(private val rootClass: ClassReference,
                             private val rootMethodId: String?,
                             private val global: ProjectClassification,
                             private val restriction: GraphRestriction
) : RestrictionFilter {
    override fun acceptClass(clazz: ClassReference): Boolean {
        if (clazz == rootClass) return true

        return clazz.accept()
    }

    private fun ClassReference.accept() =
        with(global) {
            with(restriction) {
                global.includedProjects.bySemicolon().any { path.startsWith(it) }
                        && isIncludedAndNotExcluded(classNameExcludeFilter, classNameIncludeFilter) { name }
                        && isIncludedAndNotExcluded(classPackageExcludeFilter, classPackageIncludeFilter) { path }
                        && cutTests inCase isTest()
                        && cutClient inCase isClient()
                        && cutMappings inCase isMapping()
                        && cutDataAccess inCase isDataAccess()
                        && cutDataStructures inCase isDataStructure()
                        && cutEnum inCase isEnum
                        && cutInterfaceStructures inCase isInterfaceStructure()
            }
        }

    override fun acceptMethod(method: AnalyzeMethod): Boolean {
        if (rootMethodId == method.id) return true

        return with(method) {
            with(restriction) {
                isIncludedAndNotExcluded(methodNameExcludeFilter, methodNameIncludeFilter) { name }
                        && !isJavaObjectMethod()
                        && (containingClass.classType == ClassType.Interface || cutGetterAndSetter inCase isGetterOrSetter())
                        && cutConstructors inCase isConstructor
            }
        }
    }

    override fun removeClass(clazz: ClassReference, cache: GraphCache): Boolean {
        if (rootClass == clazz) return false

        val inheritedClasses = cache.allInheritedClasses(clazz)
        val inheritedIncludingSelf = (cache.classes[clazz.id()]?.annotations
                ?: emptyList()) union inheritedClasses.flatMap { it.annotations }

        val byAnnotation = notEmptyAnd(restriction.removeByAnnotation) { reqExs ->
            reqExs.any { reqEx ->
                inheritedIncludingSelf.any { anyAnnotation -> reqEx.matches(anyAnnotation.type.name) }
            }
        }
        val byInheritance = notEmptyAnd(restriction.removeByInheritance) { reqExs ->
            reqExs.any { reqEx -> inheritedClasses.any { inherited -> reqEx.matches(inherited.reference.name) } }
        }

        val classExcluded = with(restriction) {
            !isIncludedAndNotExcluded(removeByClassPackage, "") { clazz.path }
                    || !isIncludedAndNotExcluded(removeByClassName, "") { clazz.name }
        }

        return byAnnotation || byInheritance || classExcluded
    }

}

class GraphTraversalFilter(private val rootNode: GraphNode, private val global: ProjectClassification, private val traversal: GraphTraversal) : TraversalFilter {
    override fun accept(node: GraphNode) = when (node) {
        rootNode -> true
        is AnalyzeClass -> node.reference.accept()
        is AnalyzeMethod -> node.accept() && node.containingClass.accept()
        else -> notReachable()
    }

    private fun AnalyzeMethod.accept(): Boolean {
        return with(traversal) {
            isIncludedAndNotExcluded(methodNameExcludeFilter, methodNameIncludeFilter) { name }
                    && hidePrivateMethods inCase (visibility != MethodVisibility.PUBLIC)
                    && hideInterfaceCalls inCase insideInterface()
        }
    }

    private fun ClassReference.accept(): Boolean {
        return with(global) {
            with(traversal) {
                isIncludedAndNotExcluded(classNameExcludeFilter, classNameIncludeFilter) { name }
                        && isIncludedAndNotExcluded(classPackageExcludeFilter, classPackageIncludeFilter) { path }
                        && hideDataStructures inCase isDataStructure()
                        && hideMappings inCase isMapping()
                        && hideInterfaceCalls inCase (classType == ClassType.Interface)
                        && (!onlyShowApplicationEntryPoints || isEntryPoint())
            }
        }
    }

}


private fun isIncludedAndNotExcluded(excludes: String, includes: String, extractor: () -> String) =
        emptyOr(includes) { regEx -> regEx.any { it.matches(extractor()) } }
                && emptyOr(excludes) { regEx -> regEx.none { it.matches(extractor()) } }


fun AnalyzeMethod.insideInterface() = containingClass.classType == ClassType.Interface

fun AnalyzeMethod.isGetterOrSetter(): Boolean {
    return (name.startsWith("get") && parameter.isEmpty() && !isVoid())
            || (name.startsWith("is") && parameter.isEmpty() && !isVoid())
            || (name.startsWith("has") && parameter.isEmpty() && !isVoid())
            || (name.startsWith("with") && parameter.isNotEmpty() && !isVoid())
            || (name.startsWith("set") && isVoid())
}

private fun AnalyzeMethod.isVoid() = returnTypeDisplay == null || returnTypeDisplay == "void"

fun PsiMethod.isPrivate(): Boolean {
    return !hasModifierProperty(PsiModifier.PUBLIC)
}

fun AnalyzeMethod.isJavaObjectMethod(): Boolean {
    return sequenceOf("toString", "equals", "hasCode", "getClass").contains(name)
}

