package com.example.androidgame

class DroneNetworkException(
    val canRetry: Boolean,
    cause: Throwable? = null,
) : Exception(cause)

class DroneNotFoundException : Exception()
