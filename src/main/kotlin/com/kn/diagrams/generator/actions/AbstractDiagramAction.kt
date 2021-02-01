package com.kn.diagrams.generator.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.fileTypes.FileTypeRegistry
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFileFactory
import com.kn.diagrams.generator.asyncWriteAction
import com.kn.diagrams.generator.config.DiagramConfiguration
import com.kn.diagrams.generator.findClasses
import com.kn.diagrams.generator.isJava
import com.kn.diagrams.generator.notifications.notifyErrorMissingClass


abstract class AbstractDiagramAction<T : DiagramConfiguration> : AnAction() {

    init {
        this.setInjectedContext(true)
    }

    fun generateWith(event: AnActionEvent, configuration: T) {
        event.startBackgroundAction("Generate Diagrams") { progressIndicator ->
            val diagrams: MutableMap<String, String> = mutableMapOf()

            val plantUMLDiagrams = createDiagramContent(configuration)

            for ((diagramKeyword, diagramContent) in plantUMLDiagrams) {
                val diagramFileName = configuration.rootClass.name + "_" + diagramKeyword + ".puml"
                diagrams[diagramFileName] = diagramContent

            }

            progressIndicator.fraction = 0.98

            asyncWriteAction {
                val directory = event.file().containingDirectory!!
                for ((diagramFileName, diagramContent) in diagrams) {
                    writeDiagramToFile(directory, diagramFileName, diagramContent)
                }
            }
        }
    }

    override fun actionPerformed(event: AnActionEvent) {
        generateWith(event, defaultConfiguration(event.findFirstClass()))
    }

    private fun writeDiagramToFile(directory: PsiDirectory, diagramFileName: String, diagramContent: String) {
        val umlFile = directory.findFile(diagramFileName)
        if (umlFile == null) {
            val type = FileTypeRegistry.getInstance().getFileTypeByFileName(diagramFileName)
            val newFile = PsiFileFactory.getInstance(directory.project).createFileFromText(diagramFileName, type, diagramContent)
            directory.add(newFile)
        } else {
            PsiDocumentManager.getInstance(directory.project).getDocument(umlFile.containingFile)
                    ?.setText(diagramContent)
        }
    }

    override fun update(anActionEvent: AnActionEvent) {
        val project = anActionEvent.getData(CommonDataKeys.PROJECT)
        val file = anActionEvent.getData(CommonDataKeys.PSI_FILE)

        anActionEvent.presentation.isVisible = project != null && file.isJava()
    }

    protected abstract fun defaultConfiguration(rootClass: PsiClass): T

    protected abstract fun createDiagramContent(configuration: T): List<Pair<String, String>>

}

fun AnActionEvent.findFirstClass(): PsiClass {
    val psiClass = file().findClasses().firstOrNull()

    if (psiClass == null) {
        notifyErrorMissingClass(project)
    }

    return psiClass!!
}

fun AnActionEvent.document() = PsiDocumentManager.getInstance(project!!).getDocument(file().containingFile)
fun AnActionEvent.file() = getData(CommonDataKeys.PSI_FILE)!! // ensured by update()

fun AnActionEvent.startBackgroundAction(title: String, action: (ProgressIndicator) -> Unit) {
    ProgressManager.getInstance() // make it non-blocking with progress bar
            .run(object : Task.Backgroundable(getData(CommonDataKeys.PROJECT)!!, title) {

                override fun run(progressIndicator: ProgressIndicator) {
                    progressIndicator.isIndeterminate = false
                    progressIndicator.fraction = 0.0

                    action(progressIndicator)
                }
            })
}
