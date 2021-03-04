package org.bilalkilic.kediatrhelper.listeners

import com.intellij.codeInsight.daemon.GutterIconNavigationHandler
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import org.bilalkilic.kediatrhelper.services.HandlerService
import org.bilalkilic.kediatrhelper.services.PopupService
import org.bilalkilic.kediatrhelper.utils.HandlerType
import org.bilalkilic.kediatrhelper.utils.Icons
import org.bilalkilic.kediatrhelper.utils.ItemScope
import org.bilalkilic.kediatrhelper.utils.KediatrPopupModel
import org.bilalkilic.kediatrhelper.utils.PopupConstants.POPUP_STEP_HANDLER_ASYNC
import org.bilalkilic.kediatrhelper.utils.PopupConstants.POPUP_STEP_HANDLER_BASIC
import org.bilalkilic.kediatrhelper.utils.PopupConstants.POPUP_STEP_PREFIX_NAVIGATE
import org.bilalkilic.kediatrhelper.utils.PopupConstants.POPUP_TITLE_PREFIX_MULTIPLE_HANDLERS
import org.bilalkilic.kediatrhelper.utils.PopupConstants.POPUP_TITLE_PREFIX_NEW_HANDLER
import org.bilalkilic.kediatrhelper.utils.PopupItem
import org.bilalkilic.kediatrhelper.utils.getNameFromPackage
import org.bilalkilic.kediatrhelper.utils.getSerialSuperClassNames
import org.bilalkilic.kediatrhelper.utils.isAsync
import org.jetbrains.kotlin.asJava.classes.KtLightClassForSourceDeclaration
import org.jetbrains.kotlin.idea.debugger.sequence.psi.resolveType
import org.jetbrains.kotlin.idea.refactoring.memberInfo.KtPsiClassWrapper
import org.jetbrains.kotlin.j2k.getContainingClass
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.types.typeUtil.supertypes
import org.jetbrains.kotlinx.serialization.compiler.backend.common.serialName
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
            val mainClass = KtPsiClassWrapper(elt.getContainingClass()!!).psiOrParent as KtClass

            val handlers = project.service<HandlerService>().findHandler(elt, superClassQualifiedNames, commandTypeName)
            when {
                handlers.isEmpty() -> showPopupToCreateOrNavigateHandler(mainClass, me)
                handlers.size == 1 -> handlers.first().navigate(false)
                handlers.size >= 2 -> showPopupToNavigateHandlers(mainClass, handlers, me)
            }
        }

    private fun showPopupToCreateOrNavigateHandler(mainClass: KtClass, me: MouseEvent) {
        val options = mutableListOf<PopupItem>(
            PopupItem(HandlerType.BASIC, POPUP_STEP_HANDLER_BASIC, mainClass, ItemScope.CREATE),
            PopupItem(HandlerType.ASYNC, POPUP_STEP_HANDLER_ASYNC, mainClass, ItemScope.CREATE)
        )

        val func: (PopupItem) -> Unit = {
            mainClass.project.service<PopupService>().handle(it)
        }

        val popupModel = KediatrPopupModel(
            title = POPUP_TITLE_PREFIX_NEW_HANDLER + mainClass.name,
            items = options,
            icon = Icons.createNewHandlerGutter,
            onChosenFunction = func,
            mouseEvent = me
        )
        mainClass.project.service<PopupService>().show(popupModel)
    }

    private fun showPopupToNavigateHandlers(mainClass: KtClass, handlers: List<PsiClass>, me: MouseEvent) {
        val options = mutableListOf<PopupItem>()
        handlers.forEach { handler ->
            val type = getHandlerType(handler)
            val message = POPUP_STEP_PREFIX_NAVIGATE + handler.name
            val referenceClass = getKtClass(handler)
            options.add(PopupItem(type, message, referenceClass, ItemScope.NAVIGATE))
        }
        val func: (PopupItem) -> Unit = {
            it.referenceClass.navigate(true)
        }
        val popupModel = KediatrPopupModel(
            title = POPUP_TITLE_PREFIX_MULTIPLE_HANDLERS + mainClass.name,
            items = options,
            icon = Icons.navigateToHandlerGutter,
            onChosenFunction = func,
            mouseEvent = me
        )
        mainClass.project.service<PopupService>().show(popupModel)
    }

    private fun getKtClass(psiClass: PsiClass) = (psiClass as KtLightClassForSourceDeclaration).kotlinOrigin as KtClass

    private fun getHandlerType(psiClass: PsiClass): HandlerType {
        val kediatrHandlerName = psiClass.interfaces.firstOrNull()?.name ?: return HandlerType.BASIC
        if (kediatrHandlerName.isAsync()) {
            return HandlerType.ASYNC
        }
        return HandlerType.BASIC
    }
}
