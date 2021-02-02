package org.bilalkilic.kediatrintellijplugin.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.components.service
import org.bilalkilic.kediatrintellijplugin.services.HandlerService
import org.bilalkilic.kediatrintellijplugin.utils.getSerialSuperClassNames
import org.jetbrains.kotlin.psi.KtClass

class FindHandler : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val element = e.dataContext.getData(LangDataKeys.PSI_ELEMENT)
        if (element !is KtClass) {
            return
        }

        val className = element.name ?: return
        val superClassNames = element.getSerialSuperClassNames()

        val handler = element.project.service<HandlerService>().findHandler(element, superClassNames, className)

        // TODO implement allowing user to select from many handlers of a command type
        handler.firstOrNull()?.navigate(false)
    }
}
