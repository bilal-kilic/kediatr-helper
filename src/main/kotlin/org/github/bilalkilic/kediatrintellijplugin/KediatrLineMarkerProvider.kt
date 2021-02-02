package org.github.bilalkilic.kediatrintellijplugin

import com.intellij.codeInsight.daemon.GutterIconNavigationHandler
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.psiUtil.getSuperNames
import org.github.bilalkilic.kediatrintellijplugin.services.HandlerService

class KediatrLineMarkerProvider : LineMarkerProvider {
    private val kediatrTypes = arrayOf("Command", "Query", "Notification")

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        if (element !is KtClass) {
            return null
        }

        if (kediatrTypes.none { element.getSuperNames().contains(it) }) {
            return null
        }

        return LineMarkerInfo(element, element.textRange, Icons.kediatrGutter, { "Go to Handler" }, navigationHandler(), GutterIconRenderer.Alignment.CENTER)
    }

    private fun navigationHandler(): GutterIconNavigationHandler<PsiElement> = GutterIconNavigationHandler { _, elt ->
        if (elt !is KtClass) return@GutterIconNavigationHandler

        val project = elt.project
        val handler = project.service<HandlerService>().findHandler(elt)
        handler?.navigate(false)
    }
}