package org.bilalkilic.kediatrintellijplugin.services

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
import com.jetbrains.rd.util.firstOrNull
import org.bilalkilic.kediatrintellijplugin.utils.KediatrConstants
import org.bilalkilic.kediatrintellijplugin.utils.KediatrConstants.KediatrHandlerMap
import org.jetbrains.kotlin.idea.util.projectStructure.module

@Service
class HandlerService {
    private lateinit var cachedHandlerClasses: CachedValue<Collection<PsiClass>>
    private lateinit var cachedCommandClasses: CachedValue<Collection<PsiClass>>

    fun getCachedHandlerClasses(): Collection<PsiClass> = cachedHandlerClasses.value
    fun getCachedCommandTypes(): Collection<PsiClass> = cachedCommandClasses.value

    fun findHandler(element: PsiElement, superQualifiedNames: Collection<String>, className: String): List<PsiClass> {
        val module = element.module ?: return emptyList()
        val scope = GlobalSearchScope.moduleScope(module)

        val handlerTypes = getHandlerTypes(superQualifiedNames)

        return handlerTypes.map { type ->
            ClassInheritorsSearch.search(type, scope, false).filter {
                it.superTypes.any { st ->
                    if (st is PsiClassReferenceType) st.parameters.any { p ->
                        if (p is PsiClassReferenceType) p.name == className else false
                    } else false
                }
            }
        }.flatten()
    }

    fun hasKediatrCommand(superClassQualifiedNames: Collection<String>) =
        getHandlerTypes(superClassQualifiedNames).isNotEmpty()

    private fun getHandlerTypes(superClassQualifiedNames: Collection<String>): Collection<PsiClass> =
        superClassQualifiedNames.flatMap { qualifiedName ->
            val handler = KediatrHandlerMap.filter { it.key == qualifiedName }.firstOrNull()?.value
            getCachedHandlerClasses().filter { className -> handler?.contains(className.qualifiedName) ?: false }
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
        return AllClassesSearch.search(scope, project) { KediatrConstants.KediatrHandlerNames.contains(it) }
            .findAll()
            .filter { it.qualifiedName?.startsWith(KediatrConstants.KediatrPackageName) ?: false }
            .toList()
    }

    private fun getCommandTypes(project: Project): List<PsiClass> {
        val scope = GlobalSearchScope.allScope(project)
        return AllClassesSearch.search(scope, project) { KediatrConstants.KediatrCommandNames.contains(it) }
            .findAll()
            .filter { it.qualifiedName?.startsWith(KediatrConstants.KediatrPackageName) ?: false }
            .toList()
    }

    class TreeChangeTracker : ModificationTracker {
        private val myCount: Long = 0
        override fun getModificationCount(): Long {
            return myCount
        }
    }
}
