package com.example.androidgame.data.api.dto

class DroneNetworkException(
    val canRetry: Boolean,
    cause: Throwable? = null,
) : Exception(cause)

class DroneNotFoundException : Exception()
