package org.github.bilalkilic.kediatrintellijplugin.listeners

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener
import org.github.bilalkilic.kediatrintellijplugin.services.HandlerService

class ProjectOpenedListener : ProjectManagerListener {
    override fun projectOpened(project: Project) {
        val service = project.service<HandlerService>()
        service.buildCaches(project, HandlerService.TreeChangeTracker())
        // service.getCachedHandlerClasses()
    }
}
