package com.example.androidgame.domain.model

object DroneGameEngine {

    fun missionReward(durationSeconds: Int): Int = when (durationSeconds) {
        60 -> 350
        30 -> 150
        else -> 60
    }

    fun cargoGain(durationSeconds: Int): Int = when (durationSeconds) {
        60 -> 100
        30 -> 50
        else -> 25
    }

    fun expGain(durationSeconds: Int): Int = when (durationSeconds) {
        60 -> 3
        30 -> 2
        else -> 1
    }

    fun calculateRank(currentMissions: Int, expGain: Int): Pair<Int, Int> {
        val totalMissions = currentMissions + expGain
        val newRankLevel = ((totalMissions / 3) + 1).coerceAtMost(5)
        return Pair(totalMissions, newRankLevel)
    }

    fun batteryCostForMission(durationSeconds: Int): Int = when (durationSeconds) {
        60 -> 50
        30 -> 35
        else -> 20
    }
}
