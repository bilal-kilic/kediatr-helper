package org.bilalkilic.kediatrhelper.listeners

import com.intellij.codeInsight.daemon.GutterIconNavigationHandler
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import com.intellij.psi.PsiElement
import com.intellij.ui.awt.RelativePoint
import org.bilalkilic.kediatrhelper.services.CreateNewHandlerService
import org.bilalkilic.kediatrhelper.services.HandlerService
import org.bilalkilic.kediatrhelper.utils.Icons
import org.bilalkilic.kediatrhelper.utils.PopupConstants.POPUP_STEP_HANDLER_ASYNC
import org.bilalkilic.kediatrhelper.utils.PopupConstants.POPUP_STEP_HANDLER_BASIC
import org.bilalkilic.kediatrhelper.utils.PopupConstants.POPUP_STEP_MARKER_AS_ICON
import org.bilalkilic.kediatrhelper.utils.PopupConstants.POPUP_TITLE_PREFIX
import org.bilalkilic.kediatrhelper.utils.PopupConstants.POPUP_WIDTH_MULTIPLIER
import org.bilalkilic.kediatrhelper.utils.getNameFromPackage
import org.bilalkilic.kediatrhelper.utils.getSerialSuperClassNames
import org.jetbrains.kotlin.idea.debugger.sequence.psi.resolveType
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.types.typeUtil.supertypes
import org.jetbrains.kotlinx.serialization.compiler.backend.common.serialName
import java.awt.Dimension
import java.awt.Point
import java.awt.event.MouseEvent

class KediatrHandlerClassMarker : LineMarkerProvider {
    val lineMarkerInfoInfoName = "Go to Handler"

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
            { lineMarkerInfoInfoName },
            navigationHandler(superTypeMap.first, superTypeMap.second!!),
            GutterIconRenderer.Alignment.CENTER,
            { lineMarkerInfoInfoName },
        )
    }

    private fun navigationHandler(superClassQualifiedNames: Collection<String>, commandTypeName: String) =
        GutterIconNavigationHandler<PsiElement> { me, elt ->
            val project = elt.project
            val handler = project.service<HandlerService>().findHandler(elt, superClassQualifiedNames, commandTypeName)
            handler.firstOrNull()?.navigate(false) ?: showPopupWithIcon(elt as KtClass, me)
        }

    private fun showPopupWithBasic(mainClass: KtClass, me: MouseEvent) {
        val options = listOf(POPUP_STEP_HANDLER_BASIC, POPUP_STEP_HANDLER_ASYNC)
        val title = POPUP_TITLE_PREFIX + mainClass.name
        val popup = JBPopupFactory.getInstance()
            .createPopupChooserBuilder(options.map { POPUP_STEP_MARKER_AS_ICON + it })
            .setTitle(title)
            .setMovable(false)
            .setResizable(false)
            .setRequestFocus(true)
            .setCancelOnWindowDeactivation(false)
            .setItemChosenCallback { mainClass.project.service<CreateNewHandlerService>().create(mainClass, it) }
            .createPopup()
        popup.setRequestFocus(true)
        popup.setMinimumSize(Dimension(title.length * POPUP_WIDTH_MULTIPLIER, 0))
        popup.show(RelativePoint(Point(me.xOnScreen + 30, me.yOnScreen + 20)))
    }

    private fun showPopupWithIcon(mainClass: KtClass, me: MouseEvent) {
        val title = POPUP_TITLE_PREFIX + mainClass.name
        val options = listOf(POPUP_STEP_HANDLER_BASIC, POPUP_STEP_HANDLER_ASYNC)
        val steps = object : BaseListPopupStep<String>(title, options, Icons.createNewHandlerGutter) {
            override fun onChosen(selectedValue: String, finalChoice: Boolean): PopupStep<*>? {
                mainClass.project.service<CreateNewHandlerService>().create(mainClass, selectedValue)
                return PopupStep.FINAL_CHOICE
            }
        }
        val popup = JBPopupFactory.getInstance().createListPopup(steps)
        popup.setRequestFocus(true)
        popup.setMinimumSize(Dimension(title.length * POPUP_WIDTH_MULTIPLIER, 0))
        popup.show(RelativePoint(Point(me.xOnScreen + 30, me.yOnScreen + 20)))
    }
}
