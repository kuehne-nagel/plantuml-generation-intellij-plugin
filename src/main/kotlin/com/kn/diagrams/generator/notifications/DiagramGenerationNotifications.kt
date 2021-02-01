package com.kn.diagrams.generator.notifications

import com.intellij.notification.NotificationDisplayType
import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod

fun notifyError(project: Project?, text: String) {
    if (project == null) return
    NotificationGroup("Diagram Generation plugin", NotificationDisplayType.BALLOON, true)
            .createNotification(text, NotificationType.ERROR)
            .notify(project)
}

fun notifyErrorOccurred(project: Project?) {
    notifyError(project, "Diagram could not be generated.")
}

fun notifyErrorMissingClass(project: Project?) {
    notifyError(project, "No class found for diagram generation. Please check you filter settings.")
}

fun notifyErrorMissingPublicMethod(project: Project?, rootClass: PsiClass, rootMethod: PsiMethod?) {
    notifyError(project, "no public methods found to generate diagrams in class ${rootClass.name}, method ${rootMethod?.name}")
}

