package org.bilalkilic.kediatrhelper.utils

import org.jetbrains.kotlin.psi.KtClass

data class PopupItem(
    val type: HandlerType,
    val message: String,
    val referenceClass: KtClass,
    val scope: ItemScope,
)

enum class HandlerType {
    BASIC,
    ASYNC;

    fun isAsync(): Boolean = this == ASYNC
}

enum class ItemScope {
    NAVIGATE,
    CREATE;
}
