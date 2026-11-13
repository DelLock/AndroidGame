package com.example.androidgame

import kotlinx.coroutines.delay
import retrofit2.HttpException
import java.io.IOException
import kotlin.time.Duration.Companion.milliseconds

suspend fun <T> retryIO(
    times: Int = 3,
    initialDelay: Long = 500,
    block: suspend () -> T,
): T {
    var pause = initialDelay
    repeat(times - 1) {
        try {
            return block()
        } catch (_: IOException) {
        } catch (e: HttpException) {
            if (e.code() < 500) throw e
        }
        delay(pause.milliseconds)
        pause *= 2
    }
    return block()
}
