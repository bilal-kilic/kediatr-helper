package org.bilalkilic.kediatrhelper.utils

import org.bilalkilic.kediatrhelper.utils.extensions.isAsync
import org.jetbrains.kotlin.psi.KtClass

data class PopupItem(
    val type: HandlerType,
    val message: String,
    val referenceClass: KtClass,
    val scope: ItemScope,
)

enum class HandlerType {
    BASIC,
    ASYNC,
    ;

    fun isAsync(): Boolean = this == ASYNC
}

fun figureOutHandlerType(handlerName: String?): HandlerType {
    return when {
        handlerName.isNullOrEmpty() -> HandlerType.BASIC
        handlerName.isAsync() -> HandlerType.ASYNC
        else -> HandlerType.BASIC
    }
}

enum class ItemScope {
    NAVIGATE,
    CREATE,
}
