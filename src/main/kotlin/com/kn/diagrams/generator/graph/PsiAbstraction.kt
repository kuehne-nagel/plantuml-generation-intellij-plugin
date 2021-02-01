package com.kn.diagrams.generator.graph

import com.intellij.openapi.project.ProjectManager
import com.intellij.psi.*
import com.intellij.psi.impl.source.PsiClassReferenceType
import com.intellij.psi.javadoc.PsiDocComment
import com.intellij.psi.util.PsiTypesUtil
import com.intellij.psi.util.elementType
import com.kn.diagrams.generator.cast
import com.kn.diagrams.generator.config.simpleSignature
import com.kn.diagrams.generator.escape
import com.kn.diagrams.generator.inReadAction
import com.kn.diagrams.generator.union
import org.jetbrains.java.generate.psi.PsiAdapter
import java.util.*


class AnalyzeClass(clazz: PsiClass, filter: RestrictionFilter) : GraphNode {
    val reference: ClassReference = ClassReference(clazz)
    val classType: ClassType = clazz.type()
    val fields: List<AnalyzeField>
    val methods: Map<String, AnalyzeMethod>
    val calls: Map<String, List<AnalyzeCall>>
    val superTypes: List<ClassReference> = clazz.supers.map { it.reference() }.filter { filter.acceptClass(it) }
    val annotations: List<AnalyzeAnnotation> = clazz.annotations.map { AnalyzeAnnotation(it) }

    init {
        fields = clazz.fields
                .filterNot { it.hasModifierProperty(PsiModifier.STATIC) && it !is PsiEnumConstant }
                .map { AnalyzeField(it) }

        val relevantMethods = clazz.methods.asSequence().map { it to AnalyzeMethod(it) }.filter { filter.acceptMethod(it.second) }
        methods = relevantMethods.map { it.second.id() to it.second }.toMap()

        calls = relevantMethods.asSequence()
                .map { it.first }
                .flatMap { psiMethod ->
                    val virtualInheritanceCalls = psiMethod.findSuperMethodSignaturesIncludingStatic(true).map {
                        AnalyzeCall(it.method, psiMethod, sequence = -1)
                    }.asSequence()
                    val directCalls = psiMethod.visitCalls<AnalyzeCall> { psiCall, i ->
                        val targetMethod = psiCall.resolveMethod()
                        if (targetMethod?.containingClass != null) {
                            add(AnalyzeCall(psiMethod, targetMethod, psiCall, i))

                        }
                    }

                    directCalls union virtualInheritanceCalls
                }.groupBy { it.source.classReference.id() }
    }

    fun id() = reference.id()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AnalyzeClass) return false

        if (reference != other.reference) return false

        return true
    }

    override fun hashCode(): Int {
        return reference.hashCode()
    }

    override fun toString(): String {
        return reference.name
    }

}


inline fun <T> PsiMethod.visitCalls(crossinline action: MutableList<T>.(PsiCall, Int) -> Unit): Sequence<T> {
    val callVisitor = object : PsiRecursiveElementVisitor() {
        var counter: Int = 0
        var elements: MutableList<T> = mutableListOf()
        override fun visitElement(call: PsiElement) {
            if (call is PsiCall) {
                action(elements, call, counter)
                counter++
            }
            super.visitElement(call)
        }
    }
    accept(callVisitor)

    return callVisitor.elements.asSequence()
}


class AnalyzeMethod(method: PsiMethod) : AnalyzeAttribute(method.name, method.annotationsMapped()), GraphNode {
    val id: String = method.id()
    val containingClass: ClassReference = method.containingClass!!.reference()
    val visibility: MethodVisibility = method.modifierList.visibility()
    val returnTypeDisplay: String? = method.returnType?.presentableText
    val returnTypes: List<ClassReference> = method.returnType.structureRelevantTypes().map { it.reference() }
    val parameter: List<MethodParameter> = method.parameterList.parameters
            .map { MethodParameter(it.name, it.type, it.annotationsMapped()) }
    val javaDoc = method.javaDoc()
    val isConstructor = method.isConstructor

    override fun id(): String {
        return id
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AnalyzeMethod) return false

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return id
    }
}

class AnalyzeField(field: PsiField) : Variable(field.name, field.type, field.annotationsMapped()) {
    val containingClass: ClassReference? = field.containingClass?.reference()
    val visibility: MethodVisibility = field.modifierList?.visibility() ?: MethodVisibility.PACKAGE_LOCAL
    val isEnumInstance: Boolean = field is PsiEnumConstant

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AnalyzeField

        if (containingClass != other.containingClass) return false
        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int {
        var result = containingClass?.hashCode() ?: 0
        result = 31 * result + name.hashCode()
        return result
    }

    override fun toString(): String {
        return "${containingClass?.toString()}.$name"
    }

}


class AnnotationParameter(val name: String, val value: String)

open class AnalyzeAttribute(val name: String, val annotations: List<AnalyzeAnnotation>) {
    open fun id() = name
}

fun List<AnalyzeAnnotation>.withName(name: String) = firstOrNull { it.type.name == name }


class ClassReference {
    val name: String
    val displayName: String
    val path: String
    val classType: ClassType
    val absolutePath: String?

    constructor(annotation: PsiAnnotation) {
        classType = ClassType.Class // no annotation type needed yet
        absolutePath = annotation.containingFile.originalFile.virtualFile?.canonicalPath
        name = annotation.qualifiedName?.substringAfterLast(".") ?: "no qualified name"
        displayName = name
        path = annotation.qualifiedName?.substringBeforeLast(".") ?: "no qualified name"
    }

    constructor(clazz: PsiClass) {
        classType = clazz.type()
        absolutePath = clazz.containingFile.originalFile.virtualFile?.canonicalPath
        name = clazz.qualifiedName?.substringAfterLast(".") ?: "no qualified name"
        displayName = clazz.name + (clazz.typeParameterList?.text ?: "")
        path = clazz.qualifiedName?.substringBeforeLast(".") ?: "no qualified name"
    }


    fun id() = "$path;$name"

    fun qualifiedName() = "$path.$name"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ClassReference) return false

        if (name != other.name) return false
        if (path != other.path) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + path.hashCode()
        return result
    }

    override fun toString(): String {
        return name
    }
}

class MethodReference(val classReference: ClassReference, val method: String) {
    fun id() = classReference.id().replace(";", ".") + ";" + method
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MethodReference

        if (classReference != other.classReference) return false
        if (method != other.method) return false

        return true
    }

    override fun hashCode(): Int {
        var result = classReference.hashCode()
        result = 31 * result + method.hashCode()
        return result
    }


}


enum class MethodVisibility(val value: String) {
    PUBLIC("public"),
    PROTECTED("protected"),
    PRIVATE("private"),
    PACKAGE_LOCAL("packageLocal")
}

class AnalyzeCall(callSource: PsiMethod, callTarget: PsiMethod, call: PsiCall? = null, val sequence: Int) : EdgeContext {
    val source: MethodReference = callSource.reference()
    val target: MethodReference = callTarget.reference()

    val annotations: List<AnalyzeAnnotation> = call?.parent?.children
            ?.mapNotNull { it.cast<PsiModifierList>() }
            ?.flatMap {
                it.children
                        .mapNotNull { it.cast<PsiAnnotation>() }
                        .map { AnalyzeAnnotation(it) }
            } ?: emptyList()

    fun id() = source.method + "#" + target.method

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AnalyzeCall) return false

        if (id() != other.id()) return false

        return true
    }

    override fun hashCode(): Int {
        return id().hashCode()
    }

    override fun toString(): String {
        return source.method + "->" + target.method
    }
}


fun PsiMethod.javaDoc(): String? {
    var javadoc = (docComment ?: modifierList.children.asSequence()
            .firstOrNull { it is PsiDocComment })
            ?.text
            ?.replace("\n", "&#10;")
            ?.replace("/**", "")
            ?.replace("*/", "")
            ?.replace("    ", "")
            ?.replace("  ", " ")
            ?.replace("  ", " ")
            ?.replace("*", "")

    if (javadoc == null) {
        javadoc = findSuperMethods()
                .mapNotNull {
                    (it.docComment ?: it.modifierList.children.asSequence().firstOrNull { it is PsiDocComment })
                }
                .firstOrNull()
                ?.text
                ?.replace("\n", "&#10;")
                ?.replace("/**", "")
                ?.replace("*/", "")
                ?.replace("    ", "")
                ?.replace("  ", " ")
                ?.replace("  ", " ")
                ?.replace("*", "")
    }

    return javadoc?.escape()
}

class AnalyzeAnnotation(annotation: PsiAnnotation) {
    val type: ClassReference = ClassReference(annotation)
    val parameter: List<AnnotationParameter> = annotation.parameterList.attributes
            .map {
                AnnotationParameter(it.attributeName, it.literalValue
                        ?: "")
            }

    fun attribute(name: String) = parameter.firstOrNull { it.name == name }
}

private fun PsiType.isCollectionOrMap(): Boolean {
    val project = ProjectManager.getInstance().openProjects.firstOrNull() ?: return false

    return PsiAdapter.isCollectionType(PsiElementFactory.getInstance(project), this)
            || PsiAdapter.isMapType(PsiElementFactory.getInstance(project), this)
}
abstract class Variable(val name: String, type: PsiType, val annotations: List<AnalyzeAnnotation>) {
    val types: List<ClassReference> = type.structureRelevantTypes().map { it.reference() }
    val isCollection: Boolean = type is PsiArrayType || type.isCollectionOrMap()
    val isPrimitive: Boolean = type is PsiPrimitiveType
    val typeDisplay: String = type.presentableText
}

class MethodParameter(name: String, type: PsiType, annotations: List<AnalyzeAnnotation>) : Variable(name, type, annotations)

fun PsiType?.structureRelevantTypes(): List<PsiClass> {
    if (this == null) return emptyList()

    val processed = mutableSetOf<PsiType>()
    val allTypes = mutableSetOf<PsiClass>()
    val typesToSearch = Stack<PsiType>()
    typesToSearch.push(this)

    while (typesToSearch.isNotEmpty()) {
        val current = typesToSearch.pop()
        processed.add(current)

        if (current is PsiArrayType) {
            typesToSearch.add(current.componentType)
        }

        if (current is PsiClassReferenceType) {
            typesToSearch.addAll(current.parameters)
        }

        val currentClass = PsiTypesUtil.getPsiClass(current) ?: continue
        allTypes.add(currentClass)
    }

    return allTypes.toList()
}

enum class ClassType { Interface, Enum, Class }

fun PsiClass.type() = when {
    isInterface -> ClassType.Interface
    isEnum -> ClassType.Enum
    else -> ClassType.Class
}

fun PsiClass.reference() = ClassReference(this)
fun PsiMethod.id() = containingClass?.qualifiedName + ";" + simpleSignature()
fun PsiMethod.reference() = MethodReference(containingClass!!.reference(), id())
fun PsiMethod.annotationsMapped() = annotations.map { AnalyzeAnnotation(it) }
fun PsiParameter.annotationsMapped() = annotations.map { AnalyzeAnnotation(it) }
fun PsiField.annotationsMapped() = annotations.map { AnalyzeAnnotation(it) }
fun PsiModifierList.visibility() = MethodVisibility.values().asSequence().firstOrNull { hasModifierProperty(it.value) }
        ?: MethodVisibility.PACKAGE_LOCAL
