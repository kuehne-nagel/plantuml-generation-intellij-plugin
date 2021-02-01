package com.kn.diagrams.generator.graph

import com.google.common.base.Stopwatch
import com.google.common.collect.Streams
import com.intellij.codeInsight.completion.AllClassesGetter
import com.intellij.codeInsight.completion.PlainPrefixMatcher
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.GlobalSearchScope
import com.kn.diagrams.generator.inReadAction
import com.kn.diagrams.generator.nonBlockingRead
import java.lang.RuntimeException
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.stream.Collectors
import kotlin.collections.LinkedHashSet


class GraphCache(project: Project, val filter: RestrictionFilter, searchMode: SearchMode) {

    private val LOG = Logger.getInstance("#org.plantuml.idea.com.kn.diagramgeneration.graph.GraphCache")

    val classes: MutableMap<String, AnalyzeClass> = mutableMapOf()
    val impenitenceInverted: MutableMap<String, List<ClassReference>> = mutableMapOf()
    val forwardCalls: MutableMap<String, List<AnalyzeCall>> = mutableMapOf()
    val backwardCalls: MutableMap<String, List<AnalyzeCall>> = mutableMapOf()
    val forwardMethodClassUsage: MutableMap<String, Set<MethodClassUsage>> = mutableMapOf()
    val forwardFieldClassUsage: MutableMap<ClassReference, MutableSet<FieldWithTargetType>> = mutableMapOf()
    val backwardFieldClassUsage: MutableMap<ClassReference, MutableSet<FieldWithTargetType>> = mutableMapOf()

    init {
        val allClasses = findClasses(project, searchMode, filter::acceptClass)
        addClassesToCache(allClasses)
        optimize()

        ProgressManager.getGlobalProgressIndicator()?.fraction = 0.98
    }

    private fun addClassesToCache(allClasses: MutableList<PsiClass>) {
        val stop = Stopwatch.createStarted()

        val i = AtomicInteger(0)
        val chunks = allClasses.chunked(10)
        ProgressManager.getGlobalProgressIndicator()?.text = "Cache class details"

        chunks.parallelStream().forEach { chunk ->
            if(ProgressManager.getGlobalProgressIndicator()?.isCanceled == true) throw RuntimeException("aborted")

            chunk.forEach { psiClass ->
                nonBlockingRead {
                    add(AnalyzeClass(psiClass, filter))
                }
            }

        }

        // relation to other classes is needed, so all classes must be loaded first
        removeStructurallyFilteredClasses()

        LOG.info("mapped to Ds: ${stop.elapsed(TimeUnit.MILLISECONDS)}")
    }

    private fun removeStructurallyFilteredClasses() {
        classes.entries
                .filter { (_, cls) -> filter.removeClass(cls.reference, this) }
                .forEach { (key, _) -> classes.remove(key) }
    }

    fun classFor(psiClass: PsiClass?): AnalyzeClass? {
        if (psiClass == null) return null
        return classes[psiClass.reference().id()]
    }

    fun methodFor(psiMethod: PsiMethod?): AnalyzeMethod? {
        if (psiMethod == null) return null
        return classes.values.asSequence().mapNotNull { it.methods[psiMethod.id()] }.firstOrNull()
    }

    fun allInheritedClasses(root: ClassReference): List<AnalyzeClass> {
        val inheritanceClasses = mutableSetOf<AnalyzeClass>()
        val processing = Stack<ClassReference>()
        processing.push(root)

        while (processing.isNotEmpty()) {
            val current = processing.pop()
            val analyzeClass = current.resolve() ?: continue

            if (analyzeClass.reference != root) {
                inheritanceClasses.add(analyzeClass)
            }
            processing.addAll(analyzeClass.superTypes)
        }
        return inheritanceClasses.toList()
    }

    private fun findClasses(project: Project, searchMode: SearchMode, selector: ClassReference.() -> Boolean): MutableList<PsiClass> {
        LOG.info("class collecting started")
        ProgressManager.getGlobalProgressIndicator()?.text = "Classes are collected"
        val stop = Stopwatch.createStarted()
        val classes = mutableListOf<PsiClass>()

        inReadAction {
            val scope = when (searchMode) {
                SearchMode.AllProjects -> GlobalSearchScope.allScope(project)
                else -> GlobalSearchScope.projectScope(project)
            }

            AllClassesGetter.processJavaClasses(PlainPrefixMatcher(""), project, scope) { cls ->
                if (selector(cls.reference())) {
                    classes.add(cls)
                }

                true
            }
        }

        LOG.info("find classes: ${stop.elapsed(TimeUnit.MILLISECONDS)}")

        return classes
    }

    private fun add(clazz: AnalyzeClass) {
        classes[clazz.id()] = clazz
    }

    private fun optimize() {
        val stop = Stopwatch.createStarted()
        ProgressManager.getGlobalProgressIndicator()?.text = "Datastructures getting optimized"


        val calls = classes.flatMap { it.value.calls.values.flatten() }
                .filter { call -> call.source.resolve() != null && call.target.resolve() != null }
        calls
                .groupBy { call -> call.source.method }
                .forEach { (sourceMethod, callsByMethod) ->
                    forwardCalls[sourceMethod] = callsByMethod
                }
        calls
                .groupBy { call -> call.target.method }
                .forEach { (targetMethod, calls) -> backwardCalls[targetMethod] = calls }

        classes.flatMap { sub -> sub.value.superTypes.filter { it.exists() }.map { sub.value.reference to it } }
                .groupBy { (_, superClass) -> superClass.id() }
                .mapValues { it.value.map { (subRef, _) -> subRef } }
                .forEach { (superClass, subs) -> impenitenceInverted[superClass] = subs }

        classes.values.flatMap { it.methods.values }
                .map { method ->
                    method to (method.returnTypes.filter { it.exists() && filter.acceptClass(it) }.map { MethodClassUsage(it.resolve()!!, method, "return") } union
                            method.parameter.flatMap { param -> param.types.filter { it.exists() && filter.acceptClass(it) }.map { MethodClassUsage(it.resolve()!!, method, param.name) } })
                }
                .forEach { (method, usages) ->
                    val nonSelfUsages = usages.filter { it.clazz.reference != method.containingClass }.toSet()

                    if (nonSelfUsages.isNotEmpty()) {
                        forwardMethodClassUsage[method.id] = nonSelfUsages
                    }
                }

        classes.values.flatMap { it.fields }.forEach { field ->
            field.types.asSequence()
                    .filter { it.exists() && filter.acceptClass(it) }
                    .filterNot { targetCls -> field.containingClass == targetCls }
                    .forEach { targetCls ->
                        field.containingClass?.let { cls ->
                            val fieldEdge = FieldWithTargetType(field, targetCls.resolve()!!)
                            forwardFieldClassUsage.computeIfAbsent(cls) { mutableSetOf() }.add(fieldEdge)
                            backwardFieldClassUsage.computeIfAbsent(targetCls) { mutableSetOf() }.add(fieldEdge)
                        }
                    }
        }

        LOG.info("optimization: ${stop.elapsed(TimeUnit.MILLISECONDS)}")
    }

    fun search(filter: TraversalFilter, config: SearchContext.() -> Unit): Set<List<SquashedGraphEdge>> {
        ProgressManager.getGlobalProgressIndicator()?.text = "Paths are searched and filtered"
        val stop = Stopwatch.createStarted()
        val context = SearchContext()
        config(context)

        val findings = context.roots.parallelStream().flatMap { root ->
            val forwardChains = context.forwardDepth?.takeIf { it > 0 }?.let {
                FindContext(this, Direction.Forward, filter, it, context.edgeMode).find(root)
            } ?: emptyList()
            val backwardChains = context.backwardDepth?.takeIf { it > 0 }?.let {
                FindContext(this, Direction.Backward, filter, it, context.edgeMode).find(root)
            } ?: emptyList()

            Streams.concat(forwardChains.stream(), backwardChains.stream())
        }.collect(Collectors.toCollection { LinkedHashSet<List<SquashedGraphEdge>>() })

        LOG.info("search: ${stop.elapsed(TimeUnit.MILLISECONDS)}")
        ProgressManager.getGlobalProgressIndicator()?.text = "Diagram is generated"

        return findings
    }

    private fun ClassReference.exists() = classes.containsKey(id())

    private fun MethodReference?.resolve(): AnalyzeMethod? {
        if (this == null) return null
        return classes[classReference.id()]?.methods?.get(method)
    }

    private fun ClassReference?.resolve(): AnalyzeClass? {
        if (this == null) return null
        return classes[id()]
    }
}




