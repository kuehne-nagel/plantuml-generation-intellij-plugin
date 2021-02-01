package com.kn.diagrams.generator

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiRecursiveElementVisitor
import com.intellij.util.concurrency.NonUrgentExecutor
import com.kn.diagrams.generator.config.DiagramConfiguration
import com.kn.diagrams.generator.config.loadFromMetadata
import java.util.concurrent.TimeUnit

infix fun <T> Sequence<T>.union(elements: Sequence<T>): Sequence<T> {
    return sequenceOf(this, elements).flatten()
}

// !!! do not stack read actions - when a write action comes in, it will produce a dead lock !!!
fun <T> inReadAction(action: () -> T): T { // read access needed for PSI classes/searches
    return ApplicationManager.getApplication().runReadAction<T>(action)
}

fun <T : Any> T?.toSingleList() = listOfNotNull(this)

fun notReachable(): Nothing = throw RuntimeException()

fun asyncWriteAction(action: () -> Unit){
    ApplicationManager.getApplication().invokeLater {
        ApplicationManager.getApplication().runWriteAction {
            action()
        }
    }
}

fun <K, V> MutableMap<K, MutableSet<V>>.append(key: K, value: V) {
    val entry = getOrPut(key, { mutableSetOf() })
    entry.add(value)
}

inline fun <reified T> Any?.cast(): T?{
    if(this == null) return null

    return if (this is T) this else null
}

fun <T> nonBlockingRead(action: () -> T): T? = ReadAction
        .nonBlocking<T> { action() }
        .submit(NonUrgentExecutor.getInstance())
        .blockingGet(2, TimeUnit.MINUTES)

// language.id is not set when PlantUML Integration plugin is not installed
fun PsiFile?.isPlantUML() = this?.name?.endsWith(".puml", ignoreCase = true) ?: false
fun PsiFile?.isJava() = this?.language?.id == "JAVA"

fun PsiFile.findClasses():List<PsiClass> {
    val classes = mutableListOf<PsiClass>()

    when (name.substringAfterLast(".").toLowerCase()) {
        "java" -> {
            val visitor = object : PsiRecursiveElementVisitor() {
                override fun visitElement(element: PsiElement) {
                    if (element is PsiClass) {
                        classes.add(element)
                    }
                    super.visitElement(element)
                }
            }
            accept(visitor)
        }
        "puml" -> DiagramConfiguration.loadFromMetadata(text)?.rootClass?.let { classes.add(it) }
    }

    return classes
}

infix fun Boolean.inCase(condition: Boolean) = !this || !condition

fun String.escape() = this
        .replace("<", "\\<")
        .replace(">", "\\>")
        .replace("\"", "\\\"")

fun String.escapeHTML() = this
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
