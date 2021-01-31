package org.github.bilalkilic.kediatrintellijplugin.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.PsiClassReferenceType
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.AllClassesSearch
import com.intellij.psi.search.searches.ClassInheritorsSearch
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import org.jetbrains.kotlin.idea.util.projectStructure.module
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult

@Service
class HandlerService {
    private lateinit var cachedHandlerClasses: CachedValue<Collection<PsiClass>>
    private lateinit var cachedCommandClasses: CachedValue<Collection<PsiClass>>

    fun getCachedHandlerClasses(): Collection<PsiClass> = cachedHandlerClasses.value
    fun getCachedCommandTypes(): Collection<PsiClass> = cachedCommandClasses.value

    private val kediatrPackageName = "com.trendyol.kediatr"

    private val kediatrCommandTypes = arrayOf(
        "Command",
        "Query",
        "Notification"
    )

    private val kediatrHandlerTypes = arrayOf(
        "CommandHandler",
        "AsyncCommandHandler",
        "QueryHandler",
        "AsyncQueryHandler",
        "NotificationHandler",
        "AsyncNotificationHandler",
    )

    fun findHandler(element: PsiElement, superType: String, className: String): PsiClass? {
        val module = element.module ?: return null
        val scope = GlobalSearchScope.moduleScope(module)

        val handlerTypes = getHandlerTypes(getCachedHandlerClasses(), superType)

        return handlerTypes.firstNotNullResult { type ->
            ClassInheritorsSearch.search(type, scope, false).firstOrNull {
                it.superTypes.any { st ->
                    if (st is PsiClassReferenceType) st.parameters.any { p ->
                        if (p is PsiClassReferenceType) p.name == className else false
                    } else false
                }
            }
        }
    }

    private fun getHandlerTypes(handlerClasses: Collection<PsiClass>, superType: String): Array<PsiClass> {
        return when (superType) {
            "Command" -> arrayOf(
                handlerClasses.first { it.name == "CommandHandler" },
                handlerClasses.first { it.name == "AsyncCommandHandler" },
            )
            "Query" -> arrayOf(
                handlerClasses.first { it.name == "QueryHandler" },
                handlerClasses.first { it.name == "AsyncQueryHandler" },
            )
            "Notification" -> arrayOf(
                handlerClasses.first { it.name == "NotificationHandler" },
                handlerClasses.first { it.name == "AsyncNotificationHandler" },
            )
            else -> emptyArray()
        }
    }

    fun buildCaches(project: Project, treeChangeTracker: TreeChangeTracker) {
        val manager = CachedValuesManager.getManager(project)
        val dependencies = arrayOf<Any>(PsiModificationTracker.MODIFICATION_COUNT, treeChangeTracker)

        cachedHandlerClasses = manager.createCachedValue(
            {
                CachedValueProvider.Result.create(getHandlerTypes(project), dependencies)
            },
            false
        )
        cachedCommandClasses = manager.createCachedValue(
            {
                CachedValueProvider.Result.create(getCommandTypes(project), dependencies)
            },
            false
        )
    }

    private fun getHandlerTypes(project: Project): List<PsiClass> {
        val scope = GlobalSearchScope.allScope(project)
        return AllClassesSearch.search(scope, project) { kediatrHandlerTypes.contains(it) }
            .findAll()
            .filter { it.qualifiedName?.startsWith(kediatrPackageName) ?: false }
            .toList()
    }

    private fun getCommandTypes(project: Project): List<PsiClass> {
        val scope = GlobalSearchScope.allScope(project)
        return AllClassesSearch.search(scope, project) { kediatrCommandTypes.contains(it) }
            .findAll()
            .filter { it.qualifiedName?.startsWith(kediatrPackageName) ?: false }
            .toList()
    }

    class TreeChangeTracker : ModificationTracker {
        private val myCount: Long = 0
        override fun getModificationCount(): Long {
            return myCount
        }
    }
}
