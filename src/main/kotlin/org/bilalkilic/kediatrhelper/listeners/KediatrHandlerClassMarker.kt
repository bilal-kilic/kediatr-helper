package org.bilalkilic.kediatrhelper.listeners

import com.intellij.codeInsight.daemon.GutterIconNavigationHandler
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.psi.PsiElement
import org.bilalkilic.kediatrhelper.services.HandlerService
import org.bilalkilic.kediatrhelper.utils.Icons
import org.bilalkilic.kediatrhelper.utils.getNameFromPackage
import org.bilalkilic.kediatrhelper.utils.getSerialSuperClassNames
import org.jetbrains.kotlin.idea.debugger.sequence.psi.resolveType
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.types.typeUtil.supertypes
import org.jetbrains.kotlinx.serialization.compiler.backend.common.serialName

class KediatrHandlerClassMarker : LineMarkerProvider {

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        val handlerService = element.project.service<HandlerService>()

        val superTypeMap =
            when {
                element is KtClass -> element.getSerialSuperClassNames() to element.name
                element is KtCallExpression && element.parent !is KtNamedFunction -> {
                    // get first parameter because commandBus only has one argument
                    val handlerParameter = element.valueArguments.firstOrNull()?.getArgumentExpression()?.resolveType()
                    val superClassSerialNames = handlerParameter?.supertypes()?.map { it.serialName() } ?: emptyList()
                    superClassSerialNames to handlerParameter?.serialName()?.getNameFromPackage()
                }
                else -> null
            } ?: return null

        if (!handlerService.hasKediatrCommand(superTypeMap.first)) {
            return null
        }

        return LineMarkerInfo(
            element,
            element.textRange,
            Icons.kediatrGutter,
            { "Go to Handler" },
            navigationHandler(superTypeMap.first, superTypeMap.second!!),
            GutterIconRenderer.Alignment.CENTER
        )
    }

    private fun navigationHandler(superClassQualifiedNames: Collection<String>, commandTypeName: String) =
        GutterIconNavigationHandler<PsiElement> { _, elt ->
            val project = elt.project
            val handler = project.service<HandlerService>().findHandler(elt, superClassQualifiedNames, commandTypeName)
            handler.firstOrNull()?.navigate(false)
        }
}
