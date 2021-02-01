package com.kn.diagrams.generator.sidebar

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.ContentFactory
import com.intellij.util.castSafelyTo


class DiagramGeneratorConfigurationToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val contentFactory = ContentFactory.SERVICE.getInstance()
        val content = contentFactory.createContent(
            DiagramGeneratorConfigurationToolWindow(project), "", false)

        toolWindow.contentManager.addContent(content)
    }

    companion object {
        fun instance(project: Project): DiagramGeneratorConfigurationToolWindow? {
            return ToolWindowManager.getInstance(project).getToolWindow("Diagram Generation")
                    ?.contentManager?.contents
                    ?.map { it.component }
                    ?.firstOrNull { it is DiagramGeneratorConfigurationToolWindow } // it can be JLabel "Initializing..."
                    ?.castSafelyTo()
        }
    }

}
