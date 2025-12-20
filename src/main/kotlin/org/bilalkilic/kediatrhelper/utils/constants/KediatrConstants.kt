package org.bilalkilic.kediatrhelper.utils.constants

object KediatrConstants {
    const val KEDIATR_PACKAGE_NAME = "com.trendyol.kediatr"

    val KediatrCommandNames = arrayOf(
        "Command",
        "CommandWithResult",
        "Query",
        "Notification",
    )

    val KediatrHandlerNames = arrayOf(
        "CommandHandler",
        "AsyncCommandHandler",
        "CommandWithResultHandler",
        "AsyncCommandWithResultHandler",
        "QueryHandler",
        "AsyncQueryHandler",
        "NotificationHandler",
        "AsyncNotificationHandler",
    )

    val KediatrHandlerMap = mapOf(
        "com.trendyol.kediatr.Command" to listOf(
            "com.trendyol.kediatr.CommandHandler",
            "com.trendyol.kediatr.AsyncCommandHandler"
        ),
        "com.trendyol.kediatr.CommandWithResult" to listOf(
            "com.trendyol.kediatr.CommandWithResultHandler",
            "com.trendyol.kediatr.AsyncCommandWithResultHandler",
        ),
        "com.trendyol.kediatr.Query" to listOf(
            "com.trendyol.kediatr.QueryHandler",
            "com.trendyol.kediatr.AsyncQueryHandler",
        ),
        "com.trendyol.kediatr.Notification" to listOf(
            "com.trendyol.kediatr.NotificationHandler",
            "com.trendyol.kediatr.AsyncNotificationHandler",
        ),
    )
}
