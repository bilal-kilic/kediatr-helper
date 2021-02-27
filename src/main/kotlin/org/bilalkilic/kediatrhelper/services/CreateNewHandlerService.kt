package org.bilalkilic.kediatrhelper.services

import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.ide.fileTemplates.FileTemplateUtil
import com.intellij.openapi.components.Service
import org.bilalkilic.kediatrhelper.utils.*
import org.bilalkilic.kediatrhelper.utils.TemplateFileConstants.CLASS
import org.bilalkilic.kediatrhelper.utils.TemplateFileConstants.MESSAGE
import org.bilalkilic.kediatrhelper.utils.TemplateFileConstants.RETURN
import org.bilalkilic.kediatrhelper.utils.TemplateFileConstants.SUFFIX_HANDLER
import org.bilalkilic.kediatrhelper.utils.TemplateFileConstants.TEMPLATE_FILE_COMMAND_HANDLER
import org.bilalkilic.kediatrhelper.utils.TemplateFileConstants.TEMPLATE_FILE_QUERY_HANDLER
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile

@Service
class CreateNewHandlerService {

    fun create(mainClass: KtClass) {
        val directory = mainClass.containingFile.containingDirectory
        val finalHandlerName = mainClass.name + SUFFIX_HANDLER

        val templateManager = FileTemplateManager.getInstance(mainClass.project)
        val props = templateManager.defaultProperties
        props += CLASS to finalHandlerName
        props += MESSAGE to mainClass.name

        val kediatrSuperType = mainClass.superTypeListEntries.filter {
            it.text.containsAny(KediatrConstants.KediatrCommandNames)
        }.first().text ?: return

        var templateName = ""
        when {
            kediatrSuperType.isCommand() -> {
                templateName = TEMPLATE_FILE_COMMAND_HANDLER
            }
            kediatrSuperType.isQuery() -> {
                props += RETURN to kediatrSuperType.getQueryReturnType()
                templateName = TEMPLATE_FILE_QUERY_HANDLER
            }
            else -> return
        }
        val template = templateManager.getInternalTemplate(templateName)
        val newHandlerFile = FileTemplateUtil.createFromTemplate(template, finalHandlerName, props, directory) as KtFile
        val newHandlerClass = newHandlerFile.classes.firstOrNull() ?: return
        newHandlerClass.navigate(true)
    }

}
