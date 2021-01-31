package org.github.bilalkilic.kediatrintellijplugin.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.components.service
import org.github.bilalkilic.kediatrintellijplugin.services.HandlerService
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.psiUtil.getSuperNames

class FindHandler : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val element = e.dataContext.getData(LangDataKeys.PSI_ELEMENT)
        if (element !is KtClass) {
            return
        }

        val commandTypeNames = element.project.service<HandlerService>().getCachedCommandTypes().mapNotNull { it.name }

        val kediatrSuperType = commandTypeNames.firstOrNull { element.getSuperNames().contains(it) }!!

        val handler = element.project.service<HandlerService>().findHandler(element, kediatrSuperType, element.name!!)
        handler?.navigate(false)
    }
}
