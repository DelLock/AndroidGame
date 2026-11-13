package com.example.androidgame

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue


class SoundManager {
    enum class Effect { CLICK, SUCCESS, WARNING, DELETE }

    var enabled by mutableStateOf(value = true)

    private val tone: ToneGenerator? = try {
        ToneGenerator(AudioManager.STREAM_MUSIC, 60)
    } catch (_: RuntimeException) {
        null // на некоторых устройствах/эмуляторах звук недоступен — это не ошибка
    }

    fun play(effect: Effect) {
        if (!enabled) return
        when (effect) {
            Effect.CLICK -> tone?.startTone(ToneGenerator.TONE_PROP_BEEP, 70)
            Effect.SUCCESS -> tone?.startTone(ToneGenerator.TONE_PROP_ACK, 200)
            Effect.WARNING -> tone?.startTone(ToneGenerator.TONE_PROP_NACK, 250)
            Effect.DELETE -> tone?.startTone(ToneGenerator.TONE_PROP_BEEP2, 150)
        }
    }
}
