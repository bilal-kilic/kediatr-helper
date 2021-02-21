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
import org.bilalkilic.kediatrhelper.utils.PopupConstants.POPUP_STEP_MESSAGE
import org.bilalkilic.kediatrhelper.utils.PopupConstants.POPUP_TITLE_PREFIX
import org.bilalkilic.kediatrhelper.utils.PopupConstants.POPUP_WIDTH_MULTIPLIER
import org.bilalkilic.kediatrhelper.utils.getNameFromPackage
import org.bilalkilic.kediatrhelper.utils.getSerialSuperClassNames
import org.jetbrains.kotlin.idea.debugger.sequence.psi.resolveType
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
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
            handler.firstOrNull()?.navigate(false) ?: showPopupWithIcon(elt, me)
        }

    private fun showPopup(element: PsiElement, me: MouseEvent) {
        val mainClass = (element.parent as KtFile).classes.firstOrNull() ?: return
        val title = POPUP_TITLE_PREFIX + mainClass.name
        val popup = JBPopupFactory.getInstance()
            .createPopupChooserBuilder<Any>(listOf(" + $POPUP_STEP_MESSAGE"))
            .setTitle(title)
            .setMovable(false)
            .setResizable(false)
            .setRequestFocus(true)
            .setCancelOnWindowDeactivation(false)
            .setItemChosenCallback { element.project.service<CreateNewHandlerService>().create(element) }
            .createPopup()
        popup.show(RelativePoint(Point(me.xOnScreen + 30, me.yOnScreen + 20)))
    }

    private fun showPopupWithIcon(element: PsiElement, me: MouseEvent) {
        val mainClass = (element.parent as KtFile).classes.firstOrNull() ?: return
        val title = POPUP_TITLE_PREFIX + mainClass.name
        val options = listOf(POPUP_STEP_MESSAGE)
        val step = object : BaseListPopupStep<String>(title, options, Icons.createNewHandlerGutter) {
            override fun onChosen(selectedValue: String, finalChoice: Boolean): PopupStep<*>? {
                element.project.service<CreateNewHandlerService>().create(element)
                return PopupStep.FINAL_CHOICE
            }
        }
        val popup = JBPopupFactory.getInstance().createListPopup(step)
        popup.setRequestFocus(true)
        popup.setMinimumSize(Dimension(title.length * POPUP_WIDTH_MULTIPLIER, 0))
        popup.show(RelativePoint(Point(me.xOnScreen + 30, me.yOnScreen + 20)))
    }

}
