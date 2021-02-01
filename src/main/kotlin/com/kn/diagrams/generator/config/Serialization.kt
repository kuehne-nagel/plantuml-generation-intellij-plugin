package com.kn.diagrams.generator.config

import com.google.gson.*
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.wm.WindowManager
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.GlobalSearchScope
import com.kn.diagrams.generator.cast
import com.kn.diagrams.generator.inReadAction
import java.lang.reflect.Type
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

val serializer: Gson = GsonBuilder().setVersion(1.2)
        .setPrettyPrinting()
        .registerTypeAdapter(PsiClass::class.java, PsiClassSerializer())
        .registerTypeAdapter(PsiClass::class.java, PsiClassDeserializer())
        .registerTypeAdapter(PsiMethod::class.java, PsiMethodSerializer())
        .registerTypeAdapter(PsiMethod::class.java, PsiMethodDeserializer())
        .create()

fun toJsonWithComments(config: Any) = addComments(serializer.toJson(config), config)

fun DiagramConfiguration.metaDataSection() = """
        |/' diagram meta data start
        |config=${this.javaClass.simpleName};
        |${toJsonWithComments(this)}
        |diagram meta data end '/
    """.trimMargin("|")

@OptIn(ExperimentalStdlibApi::class)
fun addComments(metadata: String, config: Any): String {
    var newMetadata = metadata
    config::class.memberProperties
            .mapNotNull { it.cast<KProperty1<Any, *>>()?.get(config) }
            .flatMap {
                it::class.java.declaredFields
                        .filter { f -> f.isAnnotationPresent(CommentWithEnumValues::class.java) || f.isAnnotationPresent(CommentWithValue::class.java) }
            }.forEach { field ->
                val comment = if (field.isAnnotationPresent(CommentWithEnumValues::class.java)) {
                    field.type.fields.joinToString(", ") { it.name }
                } else field.getAnnotation(CommentWithValue::class.java).value
                newMetadata = newMetadata.replace("\"${field.name}.*\n".toRegex()) { match -> match.value.substringBefore("\n") + " // $comment\n" }
            }

    return newMetadata
}

fun DiagramConfiguration.Companion.loadFromMetadata(diagramText: String): DiagramConfiguration? {
    val metadata = diagramText
            .substringBefore("diagram meta data end '/")
            .substringAfter("/' diagram meta data start")

    val configJson = metadata.substringAfter(";")
    val configClassName = metadata.substringBefore(";").substringAfter("config=")

    val configType = typeOf(configClassName) ?: return null
    return serializer.fromJson(configJson, configType) as DiagramConfiguration
}

fun typeOf(className: String) = sequenceOf(CallConfiguration::class.java, StructureConfiguration::class.java, FlowConfiguration::class.java)
        .firstOrNull { it.simpleName == className }

class PsiClassSerializer : JsonSerializer<PsiClass?> {
    override fun serialize(clazz: PsiClass?, type: Type, context: JsonSerializationContext): JsonElement {
        return JsonPrimitive(inReadAction { clazz?.qualifiedName })
    }
}

class PsiClassDeserializer : JsonDeserializer<PsiClass?> {
    override fun deserialize(json: JsonElement, type: Type, context: JsonDeserializationContext): PsiClass? {
        val activeProject = activeProject() ?: return null

        return inReadAction {
            JavaPsiFacade.getInstance(activeProject)
                    .findClass(json.asJsonPrimitive.asString, GlobalSearchScope.allScope(activeProject))
        }
    }
}

class PsiMethodSerializer : JsonSerializer<PsiMethod?> {
    override fun serialize(method: PsiMethod?, type: Type, context: JsonSerializationContext): JsonElement {

        return JsonPrimitive(inReadAction { method?.containingClass?.qualifiedName + "#" + method?.simpleSignature() })
    }
}

class PsiMethodDeserializer : JsonDeserializer<PsiMethod?> {

    override fun deserialize(json: JsonElement, type: Type, context: JsonDeserializationContext): PsiMethod? {
        return inReadAction {
            val activeProject = activeProject() ?: return@inReadAction null
            val fullString = json.asJsonPrimitive.asString
            val methodSignature = fullString.substringAfter("#")

            val classOfMethod = JavaPsiFacade.getInstance(activeProject)
                    .findClass(fullString.substringBefore("#"), GlobalSearchScope.allScope(activeProject))
            classOfMethod?.methods?.firstOrNull { it.simpleSignature() == methodSignature }
        }
    }
}

fun PsiMethod.simpleSignature() = "$name(${parameterList.parameters.joinToString(",") { it.type.presentableText }})"

private fun activeProject() = ProjectManager.getInstance().openProjects
        .firstOrNull { WindowManager.getInstance().suggestParentWindow(it)?.isActive == true }


@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class CommentWithEnumValues

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class CommentWithValue(val value: String)
