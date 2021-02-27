package org.bilalkilic.kediatrhelper.services

import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.ide.fileTemplates.FileTemplateUtil
import com.intellij.openapi.components.Service
import org.bilalkilic.kediatrhelper.utils.*
import org.bilalkilic.kediatrhelper.utils.TemplateFileConstants.CLASS
import org.bilalkilic.kediatrhelper.utils.TemplateFileConstants.MESSAGE
import org.bilalkilic.kediatrhelper.utils.TemplateFileConstants.RETURN
import org.bilalkilic.kediatrhelper.utils.TemplateFileConstants.SUFFIX_HANDLER
import org.bilalkilic.kediatrhelper.utils.TemplateFileConstants.SUFFIX_HANDLER_ASYNC
import org.bilalkilic.kediatrhelper.utils.TemplateFileConstants.TEMPLATE_FILE_HANDLER_COMMAND
import org.bilalkilic.kediatrhelper.utils.TemplateFileConstants.TEMPLATE_FILE_HANDLER_COMMAND_ASYNC
import org.bilalkilic.kediatrhelper.utils.TemplateFileConstants.TEMPLATE_FILE_HANDLER_QUERY
import org.bilalkilic.kediatrhelper.utils.TemplateFileConstants.TEMPLATE_FILE_HANDLER_QUERY_ASYNC
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile

@Service
class CreateNewHandlerService {

    fun create(mainClass: KtClass, selectedValue: String) {
        val directory = mainClass.containingFile.containingDirectory

        val kediatrSuperType = mainClass.superTypeListEntries.first {
            it.text.containsAny(KediatrConstants.KediatrCommandNames)
        }.text ?: return

        val templateManager = FileTemplateManager.getInstance(mainClass.project)
        val props = templateManager.defaultProperties
        props += MESSAGE to mainClass.name
        props += RETURN to kediatrSuperType.getQueryReturnType()

        val templateNameAndSuffix = getTemplateNameAndSuffix(kediatrSuperType, selectedValue) ?: return
        val finalHandlerName = mainClass.name + templateNameAndSuffix.second
        val templateName = templateNameAndSuffix.first

        props += CLASS to finalHandlerName
        val template = templateManager.getInternalTemplate(templateName)
        val newHandlerFile = FileTemplateUtil.createFromTemplate(template, finalHandlerName, props, directory)
        val newHandlerClass = (newHandlerFile as KtFile).classes.firstOrNull() ?: return
        newHandlerClass.navigate(true)
    }

    private fun getTemplateNameAndSuffix(kediatrSuperType: String, selectedValue: String): Pair<String, String>? {
        return when {
            kediatrSuperType.isCommand() -> {
                if (selectedValue.isAsync()) {
                    Pair(TEMPLATE_FILE_HANDLER_COMMAND_ASYNC, SUFFIX_HANDLER_ASYNC)
                } else {
                    Pair(TEMPLATE_FILE_HANDLER_COMMAND, SUFFIX_HANDLER)
                }
            }
            kediatrSuperType.isQuery() -> {
                if (selectedValue.isAsync()) {
                    Pair(TEMPLATE_FILE_HANDLER_QUERY_ASYNC, SUFFIX_HANDLER_ASYNC)
                } else {
                    Pair(TEMPLATE_FILE_HANDLER_QUERY, SUFFIX_HANDLER)
                }
            }
            else -> return null
        }
    }
}
