package org.bilalkilic.kediatrhelper.listeners

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar
import org.bilalkilic.kediatrhelper.services.HandlerService
import org.bilalkilic.kediatrhelper.services.PopupService
import org.bilalkilic.kediatrhelper.utils.constants.KediatrConstants.KediatrDependencyPrefix
import org.bilalkilic.kediatrhelper.utils.extensions.safeContains

class ProjectOpenedListener : ProjectManagerListener {
    override fun projectOpened(project: Project) {
        val service = project.service<HandlerService>()
        service.buildCaches(project, HandlerService.TreeChangeTracker())
        // service.getCachedHandlerClasses()
        setDependencyVersion(project)
    }

    private fun setDependencyVersion(project: Project) {
        val libraries = LibraryTablesRegistrar.getInstance().getLibraryTable(project).libraries
        val library = libraries.firstOrNull { it.name.safeContains(KediatrDependencyPrefix) }?.name ?: return
        val version = library.split(":").last()
        project.service<PopupService>().setKediatrVersion(version)
    }
}
