package org.bilalkilic.kediatrhelper.utils

import com.intellij.psi.PsiClass
import org.jetbrains.kotlin.asJava.classes.KtLightClassForSourceDeclaration
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.resolve.descriptorUtil.getAllSuperClassifiers

fun KtClassOrObject.getSerialSuperClassNames() = this.resolveToDescriptorIfAny()
    ?.getAllSuperClassifiers()
    ?.toList()
    ?.map { it.classId?.asSingleFqName()?.asString().toString() } ?: emptyList()

fun PsiClass.getKtClass() = (this as KtLightClassForSourceDeclaration).kotlinOrigin as KtClass
