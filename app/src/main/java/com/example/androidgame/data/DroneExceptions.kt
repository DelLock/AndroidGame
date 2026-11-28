package com.example.androidgame.data

enum class NetworkErrorReason(val userMessage: String) {
    NO_NETWORK("Нет соединения с сетью"),
    SERVER_ERROR("Ошибка сервера"),
    RATE_LIMIT("Превышен лимит запросов"),
    FORMAT_ERROR("Ошибка формата ответа сервера"),
    OTHER("Ошибка соединения"),
}

class DroneNetworkException(
    val reason: NetworkErrorReason,
    val canRetry: Boolean,
    cause: Throwable? = null,
) : Exception(reason.userMessage, cause)

class DroneNotFoundException : Exception()
