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
        val ktClass = (e.dataContext.getData(LangDataKeys.PSI_ELEMENT) ?: return) as KtClass

        val commandTypeNames = ktClass.project.service<HandlerService>().getCachedCommandTypes().mapNotNull { it.name }

        val kediatrSuperType = commandTypeNames.firstOrNull { ktClass.getSuperNames().contains(it) }!!

        val handler = ktClass.project.service<HandlerService>().findHandler(ktClass, kediatrSuperType, ktClass.name!!)
        handler?.navigate(false)
    }
}
