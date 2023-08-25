package org.bilalkilic.kediatrhelper.services

import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.ide.fileTemplates.FileTemplateUtil
import com.intellij.openapi.components.Service
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import com.intellij.ui.awt.RelativePoint
import org.bilalkilic.kediatrhelper.utils.ItemScope
import org.bilalkilic.kediatrhelper.utils.KediatrPopupModel
import org.bilalkilic.kediatrhelper.utils.PopupItem
import org.bilalkilic.kediatrhelper.utils.constants.Icons
import org.bilalkilic.kediatrhelper.utils.constants.KediatrConstants
import org.bilalkilic.kediatrhelper.utils.constants.PopupConstants.POPUP_WIDTH_MULTIPLIER
import org.bilalkilic.kediatrhelper.utils.constants.PopupConstants.XAxisPadding
import org.bilalkilic.kediatrhelper.utils.constants.PopupConstants.YAxisPadding
import org.bilalkilic.kediatrhelper.utils.constants.TemplateFileConstants
import org.bilalkilic.kediatrhelper.utils.extensions.*
import org.jetbrains.kotlin.psi.KtFile
import java.awt.Dimension
import java.awt.Point
import javax.swing.Icon

@Service
class PopupService {
    fun show(popupModel: KediatrPopupModel) {
        with(popupModel) {
            val steps = object : BaseListPopupStep<PopupItem>(title, items.toList(), icon) {
                override fun getTextFor(value: PopupItem): String {
                    return value.message
                }

                override fun getIconFor(value: PopupItem): Icon {
                    return if (value.scope == ItemScope.NAVIGATE) Icons.navigateToHandlerGutter
                    else Icons.createNewHandlerGutter
                }

                override fun onChosen(selectedValue: PopupItem, finalChoice: Boolean): PopupStep<*>? {
                    onChosenFunction.invoke(selectedValue)
                    return PopupStep.FINAL_CHOICE
                }
            }
            val popup = JBPopupFactory.getInstance().createListPopup(steps)
            popup.setRequestFocus(true)
            popup.setMinimumSize(Dimension(title.length * POPUP_WIDTH_MULTIPLIER, 0))
            popup.show(RelativePoint(Point(mouseEvent.xOnScreen + XAxisPadding, mouseEvent.yOnScreen + YAxisPadding)))
        }
    }

    fun handle(popupItem: PopupItem) {
        when (popupItem.scope) {
            ItemScope.CREATE -> createNewHandler(popupItem)
            ItemScope.NAVIGATE -> popupItem.referenceClass.navigate(true)
        }
    }

    private fun createNewHandler(popupItem: PopupItem) {
        val mainClass = popupItem.referenceClass
        val directory = mainClass.containingFile.containingDirectory

        val kediatrSuperType = mainClass.superTypeListEntries.first {
            it.text.containsAny(KediatrConstants.KediatrCommandNames)
        }.text ?: return

        val templateManager = FileTemplateManager.getInstance(mainClass.project)
        val props = templateManager.defaultProperties
        props += TemplateFileConstants.MESSAGE to mainClass.name
        props += TemplateFileConstants.RETURN to kediatrSuperType.getQueryReturnType()

        val templateNameAndSuffix = getTemplateNameAndSuffix(kediatrSuperType, popupItem) ?: return
        val finalHandlerName = mainClass.name + templateNameAndSuffix.second
        val templateName = templateNameAndSuffix.first

        props += TemplateFileConstants.CLASS to finalHandlerName
        val template = templateManager.getInternalTemplate(templateName)
        val newHandlerFile = FileTemplateUtil.createFromTemplate(template, finalHandlerName, props, directory)
        val newHandlerClass = (newHandlerFile as KtFile).classes.firstOrNull() ?: return
        newHandlerClass.navigate(true)
    }

    private fun getTemplateNameAndSuffix(kediatrSuperType: String, selectedValue: PopupItem): Pair<String, String>? {
        return when {
            kediatrSuperType.isCommandWithResult() -> {
                if (selectedValue.type.isAsync()) {
                    Pair(
                        TemplateFileConstants.TEMPLATE_FILE_HANDLER_COMMAND_WITH_RESULT_ASYNC,
                        TemplateFileConstants.SUFFIX_HANDLER_ASYNC
                    )
                } else {
                    Pair(
                        TemplateFileConstants.TEMPLATE_FILE_HANDLER_COMMAND_WITH_RESULT,
                        TemplateFileConstants.SUFFIX_HANDLER
                    )
                }
            }

            kediatrSuperType.isCommand() -> {
                if (selectedValue.type.isAsync()) {
                    Pair(
                        TemplateFileConstants.TEMPLATE_FILE_HANDLER_COMMAND_ASYNC,
                        TemplateFileConstants.SUFFIX_HANDLER_ASYNC
                    )
                } else {
                    Pair(TemplateFileConstants.TEMPLATE_FILE_HANDLER_COMMAND, TemplateFileConstants.SUFFIX_HANDLER)
                }
            }

            kediatrSuperType.isQuery() -> {
                if (selectedValue.type.isAsync()) {
                    Pair(
                        TemplateFileConstants.TEMPLATE_FILE_HANDLER_QUERY_ASYNC,
                        TemplateFileConstants.SUFFIX_HANDLER_ASYNC
                    )
                } else {
                    Pair(TemplateFileConstants.TEMPLATE_FILE_HANDLER_QUERY, TemplateFileConstants.SUFFIX_HANDLER)
                }
            }

            kediatrSuperType.isNotification() -> {
                if (selectedValue.type.isAsync()) {
                    Pair(
                        TemplateFileConstants.TEMPLATE_FILE_HANDLER_NOTIFICATION_ASYNC,
                        TemplateFileConstants.SUFFIX_HANDLER_ASYNC
                    )
                } else {
                    Pair(TemplateFileConstants.TEMPLATE_FILE_HANDLER_NOTIFICATION, TemplateFileConstants.SUFFIX_HANDLER)
                }
            }

            else -> return null
        }
    }
}
