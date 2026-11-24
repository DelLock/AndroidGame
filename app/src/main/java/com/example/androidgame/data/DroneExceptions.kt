package com.example.androidgame.data

class DroneNetworkException(
    val canRetry: Boolean,
    cause: Throwable? = null,
) : Exception(cause)

class DroneNotFoundException : Exception()
