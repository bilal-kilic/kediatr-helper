package org.bilalkilic.kediatrhelper.utils.extensions

fun String.getReturnType(): String {
    val r = Regex("(?<=Query|CommandWithResult<)(.*)(?=>)")
    val matches = r.find(this)
    return matches?.groupValues?.first() ?: ""
}

fun String.containsAny(array: Array<String>) = array.any { this.contains(it) }

fun String.isAsync() = this.contains("Async")

fun String.isQuery() = this.contains("Query")

fun String.isNotification() = this.contains("Notification")

fun String.isCommand() = this.contains("Command")

fun String.isCommandWithResult() = this.contains("CommandWithResult")

fun String.getClassNameFromPackage() = split(".").last()
