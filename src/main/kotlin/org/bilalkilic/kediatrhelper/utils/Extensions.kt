package org.bilalkilic.kediatrhelper.utils

import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.resolve.descriptorUtil.getAllSuperClassifiers

fun String.getNameFromPackage() = split(".").last()

fun KtClassOrObject.getSerialSuperClassNames() = this.resolveToDescriptorIfAny()
    ?.getAllSuperClassifiers()
    ?.toList()
    ?.map { it.classId?.asSingleFqName()?.asString().toString() } ?: emptyList()

fun String.getQueryReturnType(): String {
    val r = Regex("(?<=Query<)(.*)(?=>)")
    val matches = r.find(this)
    return matches?.groupValues?.first() ?: ""
}

fun String.isQuery() = this.contains("Query")
