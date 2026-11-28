package com.example.androidgame.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Drone(
    val id: Int,
    val name: String,
    val baseCity: String = "Москва",
    val rankLevel: Int = 1,
    val battery: Int = 100,
    val maxBattery: Int = 100,
    val cargo: Int = 0,
    val maxCargo: Int = 100,
    val temperature: Int = 20,
    val heatRate: Int = 15,
    val missionsCompleted: Int = 0,
    val activeMissionDuration: Int = 0,
    val activeMissionRemaining: Int = 0,
    val missionIsStormy: Boolean = false,
) {
    val status: Status
        get() = when {
            activeMissionRemaining > 0 -> Status.ON_MISSION
            temperature >= 80 -> Status.OVERHEATED
            battery <= 10 -> Status.DEPLETED
            cargo >= maxCargo -> Status.FULL_CARGO
            else -> Status.NORMAL
        }

    val rankName: String
        get() = when (rankLevel) {
            1 -> "Новобранец"
            2 -> "Оператор"
            3 -> "Ветеран"
            4 -> "Спецназ"
            else -> "Ас флота"
        }

    val upgradeCost: Int
        get() = 120 * rankLevel

    val batteryBoostAmount: Int
        get() = 20 * rankLevel

    val cargoBoostAmount: Int
        get() = 25 * rankLevel

    val coolingBoostAmount: Int
        get() = (2 * rankLevel).coerceAtMost(5)

    fun missionLaunchCost(durationSeconds: Int): Int = when (durationSeconds) {
        60 -> 150 * rankLevel
        30 -> 70 * rankLevel
        else -> 30 * rankLevel
    }

    val canCollectData: Boolean
        get() = (status != Status.ON_MISSION) && (status != Status.OVERHEATED) && (status != Status.DEPLETED) && (cargo < maxCargo)

    val canTransmitData: Boolean
        get() = (status != Status.ON_MISSION) && (cargo > 0) && (battery > 10)

    val canHibernate: Boolean
        get() = (status != Status.ON_MISSION) && ((battery < maxBattery) || (temperature > 20))

    fun canStartMission(durationSeconds: Int, currentCredits: Int): Boolean {
        val cost = missionLaunchCost(durationSeconds)
        return (status != Status.ON_MISSION) && (status != Status.OVERHEATED) && (battery >= 30) && (currentCredits >= cost)
    }

    enum class Status { NORMAL, ON_MISSION, OVERHEATED, DEPLETED, FULL_CARGO }
}

fun startDrones(): List<Drone> = List(12) { i ->
    val id = i + 1
    val base = ALL_BASES[i % ALL_BASES.size]
    val initialBattery = when (i % 4) {
        1 -> 10
        else -> 100
    }
    val initialTemp = when (i % 4) {
        2 -> 85
        else -> 20
    }
    val initialCargo = when (i % 4) {
        3 -> 100
        else -> 0
    }
    Drone(
        id = id,
        name = "Дрон X-%02d".format(id),
        baseCity = base.city,
        battery = initialBattery,
        cargo = initialCargo,
        temperature = initialTemp,
    )
}
