package org.github.bilalkilic.kediatrintellijplugin

import com.intellij.codeInsight.daemon.GutterIconNavigationHandler
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.psi.PsiElement
import org.github.bilalkilic.kediatrintellijplugin.services.HandlerService
import org.jetbrains.kotlin.idea.debugger.sequence.psi.resolveType
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.psiUtil.getSuperNames
import org.jetbrains.kotlin.types.typeUtil.supertypes
import org.jetbrains.kotlinx.serialization.compiler.backend.common.serialName

class KediatrHandlerClassMarker : LineMarkerProvider {

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        val commandTypes = element.project.service<HandlerService>()
            .getCachedCommandTypes()

        val superTypeMap = when (element) {
            is KtClass -> {
                val commandTypeNames = commandTypes.mapNotNull { it.name }
                val kediatrSuperType = commandTypeNames.firstOrNull { element.getSuperNames().contains(it) }

                kediatrSuperType to element.name!!
            }
            is KtCallExpression -> {
                val commandTypesNames = commandTypes.map { it.qualifiedName }

                // get first parameter because commandBus only has one argument
                val handlerParameter = element.valueArguments.first().getArgumentExpression()?.resolveType()
                val kediatrSuperType = commandTypesNames.firstOrNull { ctn ->
                    handlerParameter?.supertypes()?.any { ctn == it.serialName() } ?: false
                }

                kediatrSuperType?.getNameFromPackage() to handlerParameter!!.serialName().getNameFromPackage()
            }
            else -> null
        }

        if (superTypeMap?.first == null) {
            return null
        }

        return LineMarkerInfo(
            element,
            element.textRange,
            Icons.kediatrGutter,
            { "Go to Handler" },
            navigationHandler(superTypeMap.first!!, superTypeMap.second),
            GutterIconRenderer.Alignment.CENTER
        )
    }

    private fun navigationHandler(superTypeName: String, commandTypeName: String) =
        GutterIconNavigationHandler<PsiElement> { _, elt ->
            val project = elt.project
            val handler = project.service<HandlerService>().findHandler(elt, superTypeName, commandTypeName)
            handler?.navigate(false)
        }
}
