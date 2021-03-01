package org.bilalkilic.kediatrhelper.utils

data class PopupStepForHandler(val type: HandlerType, val message: String)

enum class HandlerType {
    BASIC,
    ASYNC;

    fun isAsync(): Boolean = this == ASYNC
}