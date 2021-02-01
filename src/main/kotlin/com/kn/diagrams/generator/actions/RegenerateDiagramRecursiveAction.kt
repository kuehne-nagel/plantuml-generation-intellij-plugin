package com.kn.diagrams.generator.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.psi.impl.file.PsiJavaDirectoryImpl
import com.intellij.util.castSafelyTo

class RegenerateDiagramRecursiveAction : RegenerateDiagramAction() {

    init {
        processDirectoriesRecursive = true
    }

    override fun update(anActionEvent: AnActionEvent) {
        val project = anActionEvent.getData(CommonDataKeys.PROJECT)
        val directory = anActionEvent.getData(CommonDataKeys.PSI_ELEMENT).castSafelyTo<PsiJavaDirectoryImpl>()

        anActionEvent.presentation.isVisible = project != null && directory?.isDirectory == true
    }
}
