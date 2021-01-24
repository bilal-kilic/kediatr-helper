package org.github.bilalkilic.kediatrintellijplugin.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import com.intellij.psi.PsiClass
import com.intellij.psi.impl.source.PsiClassReferenceType
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.AllClassesSearch
import com.intellij.psi.search.searches.ClassInheritorsSearch
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import org.jetbrains.kotlin.idea.util.projectStructure.module
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.psiUtil.getSuperNames
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult

@Service
class HandlerService {
    private lateinit var cachedHandlerClasses: CachedValue<Collection<PsiClass>>
    fun getCachedHandlerClasses(): Collection<PsiClass> = cachedHandlerClasses.value

    fun findHandler(ktClass: KtClass): PsiClass? {
        val module = ktClass.module ?: return null
        val scope = GlobalSearchScope.moduleScope(module)

        val handlerTypes = getHandlerTypes(getCachedHandlerClasses(), ktClass)

        return handlerTypes.firstNotNullResult { type ->
            ClassInheritorsSearch.search(type, scope, false).firstOrNull {
                it.superTypes.any { st ->
                    if (st is PsiClassReferenceType) st.parameters.any { p ->
                        if (p is PsiClassReferenceType) p.name == ktClass.name else false
                    } else false
                }
            }
        }
    }

    private fun getHandlerTypes(handlerClasses: Collection<PsiClass>, ktClass: KtClass): Array<PsiClass> {
        val superNames = ktClass.getSuperNames()
        return when {
            superNames.contains("Command") -> arrayOf(
                handlerClasses.first { it.name == "CommandHandler" },
                handlerClasses.first { it.name == "AsyncCommandHandler" },
            )
            superNames.contains("Query") -> arrayOf(
                handlerClasses.first { it.name == "QueryHandler" },
                handlerClasses.first { it.name == "AsyncQueryHandler" },
            )
            superNames.contains("Notification") -> arrayOf(
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
    }

    private fun getHandlerTypes(project: Project): List<PsiClass> {
        val scope = GlobalSearchScope.allScope(project)
        return AllClassesSearch.search(scope, project) { it.contains("Handler") }
            .findAll()
            .filter {
                it.qualifiedName?.startsWith("com.trendyol.kediatr") ?: false &&
                    it.qualifiedName?.endsWith("Handler") ?: false
            }
            .toList()
    }

    class TreeChangeTracker : ModificationTracker {
        private val myCount: Long = 0
        override fun getModificationCount(): Long {
            return myCount
        }
    }
}
