package org.github.bilalkilic.kediatrintellijplugin.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.components.service
import org.github.bilalkilic.kediatrintellijplugin.services.HandlerService
import org.jetbrains.kotlin.psi.KtClass

class FindHandler : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val ktClass = (e.dataContext.getData(LangDataKeys.PSI_ELEMENT) ?: return) as KtClass

        val handler = ktClass.project.service<HandlerService>().findHandler(ktClass)
        handler?.navigate(false)
    }
}
