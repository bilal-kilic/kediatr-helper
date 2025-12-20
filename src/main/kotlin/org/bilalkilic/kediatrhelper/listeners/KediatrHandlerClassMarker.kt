package org.bilalkilic.kediatrhelper.listeners

import com.intellij.codeInsight.daemon.GutterIconNavigationHandler
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import org.bilalkilic.kediatrhelper.services.HandlerService
import org.bilalkilic.kediatrhelper.services.PopupService
import org.bilalkilic.kediatrhelper.utils.HandlerType
import org.bilalkilic.kediatrhelper.utils.ItemScope
import org.bilalkilic.kediatrhelper.utils.KediatrPopupModel
import org.bilalkilic.kediatrhelper.utils.PopupItem
import org.bilalkilic.kediatrhelper.utils.constants.Icons
import org.bilalkilic.kediatrhelper.utils.constants.PopupConstants.POPUP_STEP_HANDLER_ASYNC
import org.bilalkilic.kediatrhelper.utils.constants.PopupConstants.POPUP_STEP_HANDLER_BASIC
import org.bilalkilic.kediatrhelper.utils.constants.PopupConstants.POPUP_STEP_PREFIX_NAVIGATE
import org.bilalkilic.kediatrhelper.utils.constants.PopupConstants.POPUP_TITLE_PREFIX_FOR_MULTIPLE_HANDLERS
import org.bilalkilic.kediatrhelper.utils.constants.PopupConstants.POPUP_TITLE_PREFIX_FOR_NO_HANDLER
import org.bilalkilic.kediatrhelper.utils.extensions.getClassNameFromPackage
import org.bilalkilic.kediatrhelper.utils.extensions.getKtClass
import org.bilalkilic.kediatrhelper.utils.extensions.getSerialSuperClassNames
import org.bilalkilic.kediatrhelper.utils.figureOutHandlerType
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.types.symbol
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtNamedFunction
import java.awt.event.MouseEvent

class KediatrHandlerClassMarker : LineMarkerProvider {
    private val lineMarkerInfoInfoName = "Go to Handler"

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        return try {
            getLineMarkerInfoInternal(element)
        } catch (e: Exception) {
            // Log exceptions but don't crash
            null
        }
    }

    private fun getLineMarkerInfoInternal(element: PsiElement): LineMarkerInfo<*>? {
        val handlerService = element.project.service<HandlerService>()

        val superTypeMap =
            when {
                element.parent is KtNamedFunction -> return null
                element is KtClass -> {
                    val superClassSerialNames = element.getSerialSuperClassNames()

                    // Get super type names with class' own name
                    val commandTypeClassNames =
                        superClassSerialNames
                            .map { it.getClassNameFromPackage() }
                            .plus(element.name)
                            .filterNotNull()

                    superClassSerialNames to commandTypeClassNames
                }

                element is KtCallExpression -> {
                    // With K2 Analysis API take the tip info from KtCallExpression
                    val superTypeInfo = getCallExpressionSuperTypes(element) ?: return null
                    superTypeInfo
                }

                else -> return null
            }

        if (!handlerService.hasKediatrCommand(superTypeMap.first)) {
            return null
        }

        return LineMarkerInfo(
            element,
            element.textRange,
            Icons.kediatrGutter,
            { lineMarkerInfoInfoName },
            navigationHandler(superTypeMap.first, superTypeMap.second),
            GutterIconRenderer.Alignment.CENTER,
            { lineMarkerInfoInfoName },
        )
    }

    private fun getCallExpressionSuperTypes(element: KtCallExpression): Pair<List<String>, List<String>>? {
        val argumentExpression = element.valueArguments.firstOrNull()?.getArgumentExpression()
            ?: return null

        return analyze(argumentExpression) {
            val ktType = argumentExpression.expressionType ?: return@analyze null

            val superClassSerialNames = ktType.allSupertypes.mapNotNull { superType ->
                val classSymbol = superType.symbol as? KaClassSymbol
                classSymbol?.classId?.asSingleFqName()?.asString()
            }.toList()

            val ownClassSymbol = ktType.symbol as? KaClassSymbol
            val ownTypeName = ownClassSymbol?.classId?.asSingleFqName()?.asString()

            // Get super type names with class' own name
            val commandTypeClassNames = superClassSerialNames
                .plus(ownTypeName)
                .filterNotNull()
                .map { it.getClassNameFromPackage() }

            superClassSerialNames to commandTypeClassNames
        }
    }

    private fun navigationHandler(
        superClassQualifiedNames: Collection<String>,
        commandTypeNames: List<String>,
    ) = GutterIconNavigationHandler<PsiElement> { me, elt ->
        val project = elt.project
        val handlers =
            project
                .service<HandlerService>()
                .findHandler(elt, superClassQualifiedNames, commandTypeNames)

        when {
            handlers.isEmpty() -> if (elt is KtClass) showPopupToCreateHandler(elt, me)
            handlers.size >= 2 -> showPopupToNavigateHandlers(project, handlers, me)
            handlers.size == 1 -> handlers.first().navigate(false)
        }
    }

    private fun showPopupToCreateHandler(
        mainClass: KtClass,
        mouseEvent: MouseEvent,
    ) {
        val options =
            mutableListOf(
                PopupItem(HandlerType.BASIC, POPUP_STEP_HANDLER_BASIC, mainClass, ItemScope.CREATE),
                PopupItem(HandlerType.ASYNC, POPUP_STEP_HANDLER_ASYNC, mainClass, ItemScope.CREATE),
            )

        val popupModel =
            KediatrPopupModel(
                title = POPUP_TITLE_PREFIX_FOR_NO_HANDLER,
                items = options,
                icon = Icons.navigateToHandlerGutter,
                onChosenFunction = { mainClass.project.service<PopupService>().handle(it) },
                mouseEvent = mouseEvent,
            )

        mainClass.project.service<PopupService>().show(popupModel)
    }

    private fun showPopupToNavigateHandlers(
        project: Project,
        handlers: List<PsiClass>,
        mouseEvent: MouseEvent,
    ) {
        val options = mutableListOf<PopupItem>()
        handlers.forEach { handler ->
            val handlerName =
                handler
                    .interfaces
                    .firstOrNull()
                    ?.name

            val handlerType = figureOutHandlerType(handlerName)
            val message = POPUP_STEP_PREFIX_NAVIGATE + handler.name
            val referenceClass = handler.getKtClass() ?: return@forEach

            options.add(PopupItem(handlerType, message, referenceClass, ItemScope.NAVIGATE))
        }

        val popupModel =
            KediatrPopupModel(
                title = POPUP_TITLE_PREFIX_FOR_MULTIPLE_HANDLERS,
                items = options,
                icon = Icons.navigateToHandlerGutter,
                onChosenFunction = { it.referenceClass.navigate(true) },
                mouseEvent = mouseEvent,
            )

        project.service<PopupService>().show(popupModel)
    }
}
