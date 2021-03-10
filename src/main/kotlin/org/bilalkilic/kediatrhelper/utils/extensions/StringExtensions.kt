package org.bilalkilic.kediatrhelper.utils

fun String.getQueryReturnType(): String {
    val r = Regex("(?<=Query<)(.*)(?=>)")
    val matches = r.find(this)
    return matches?.groupValues?.first() ?: ""
}

fun String.containsAny(array: Array<String>) = array.any { this.contains(it) }

fun String.isAsync() = this.contains("Async")

fun String.isQuery() = this.contains("Query")

fun String.isNotification() = this.contains("Notification")

fun String.isCommand() = this.contains("Command")

fun String.getClassNameFromPackage() = split(".").last()
