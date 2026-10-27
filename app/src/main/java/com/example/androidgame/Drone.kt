package com.example.androidgame

data class Drone(
    val id: Int,
    val name: String,
    val battery: Int = 100,
    val cargo: Int = 0,
    val temperature: Int = 20
) {
    val status: Status
        get() = when {
            temperature >= 80 -> Status.OVERHEATED
            battery <= 10 -> Status.DEPLETED
            cargo >= 100 -> Status.FULL_CARGO
            else -> Status.NORMAL
        }

    val canCollectData: Boolean
        get() = status != Status.OVERHEATED && status != Status.DEPLETED && cargo < 100

    val canTransmitData: Boolean
        get() = cargo > 0 && battery > 20

    val canHibernate: Boolean
        get() = battery < 100 || temperature > 20

    enum class Status { NORMAL, OVERHEATED, DEPLETED, FULL_CARGO }
}

fun startDrones(): List<Drone> = List(12) { i ->
    val id = i + 1
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
        name = "Дрон-разведчик X-%02d".format(id),
        battery = initialBattery,
        cargo = initialCargo,
        temperature = initialTemp
    )
}
