package com.example.androidgame

data class Drone(
    val id: Int,
    val name: String,
    val battery: Int = 100,
    val cargo: Int = 0,
    val temperature: Int = 20,
) {
    val status: Status
        get() = when {
            temperature >= 80 -> Status.OVERHEATED
            battery <= 10 -> Status.DEPLETED
            cargo >= 100 -> Status.FULL_CARGO
            else -> Status.NORMAL
        }

    val canCollectData: Boolean
        get() = (status != Status.OVERHEATED) && (status != Status.DEPLETED) && (cargo < 100)

    val canTransmitData: Boolean
        get() = cargo > 0 && battery > 10

    val canHibernate: Boolean
        get() = battery < 100 || temperature > 20

    enum class Status { NORMAL, OVERHEATED, DEPLETED, FULL_CARGO }
}

fun startDrones(): List<Drone> = listOf(
    Drone(id = 1, name = "Дрон-разведчик X-01", battery = 100, cargo = 0, temperature = 20),
    Drone(id = 2, name = "Дрон-разведчик X-02", battery = 85, cargo = 25, temperature = 35),
)
