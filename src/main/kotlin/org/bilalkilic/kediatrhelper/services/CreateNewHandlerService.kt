package org.bilalkilic.kediatrhelper.services

import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.ide.fileTemplates.FileTemplateUtil
import com.intellij.openapi.components.Service
import com.intellij.psi.PsiElement
import org.bilalkilic.kediatrhelper.utils.TemplateFileConstants.CLASS
import org.bilalkilic.kediatrhelper.utils.TemplateFileConstants.MESSAGE
import org.bilalkilic.kediatrhelper.utils.TemplateFileConstants.RETURN
import org.bilalkilic.kediatrhelper.utils.TemplateFileConstants.CLASS_NAME_SUFFIX
import org.bilalkilic.kediatrhelper.utils.TemplateFileConstants.TEMPLATE_FILE_COMMAND_HANDLER
import org.bilalkilic.kediatrhelper.utils.TemplateFileConstants.TEMPLATE_FILE_QUERY_HANDLER
import org.bilalkilic.kediatrhelper.utils.getQueryReturnType
import org.bilalkilic.kediatrhelper.utils.isQuery
import org.jetbrains.kotlin.psi.KtFile

@Service
class CreateNewHandlerService {

    fun create(element: PsiElement) {
        val directory = element.containingFile.containingDirectory
        val mainClass = (element.parent as KtFile).classes.firstOrNull() ?: return
        val mainClassName = mainClass.name
        val finalClassName = mainClassName + CLASS_NAME_SUFFIX
        val messageType = mainClass.interfaces.firstOrNull()?.name ?: return

        val templateManager = FileTemplateManager.getInstance(element.project)
        val props = templateManager.defaultProperties
        props += CLASS to finalClassName
        props += MESSAGE to mainClassName

        var templateName = TEMPLATE_FILE_COMMAND_HANDLER
        if (messageType.isQuery()) {
            props += RETURN to element.text.getQueryReturnType()
            templateName = TEMPLATE_FILE_QUERY_HANDLER
        }

        val template = templateManager.getInternalTemplate(templateName)
        val newHandlerFile = FileTemplateUtil.createFromTemplate(template, finalClassName, props, directory)
        (newHandlerFile as KtFile).classes.first().navigate(true)
    }

}
