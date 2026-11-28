package com.example.androidgame

import com.example.androidgame.data.DroneRepository
import com.example.androidgame.data.api.OpenMeteoApi
import com.example.androidgame.data.api.dto.CurrentWeatherDto
import com.example.androidgame.data.api.dto.OpenMeteoResponseDto
import com.example.androidgame.domain.model.Drone
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeOpenMeteoApi : OpenMeteoApi {
    override suspend fun getWeather(
        latitude: Double,
        longitude: Double,
        currentWeather: Boolean,
    ): OpenMeteoResponseDto {
        return OpenMeteoResponseDto(CurrentWeatherDto(20.0, 5.0, 0))
    }
}

class DroneRepositoryTest {

    private lateinit var repo: DroneRepository

    @Before
    fun setUp() {
        repo = DroneRepository(
            openMeteoApi = FakeOpenMeteoApi(),
            mainContext = Dispatchers.Default,
        )
    }

    @Test
    fun initialDronesCountIsAtLeastTen() {
        assertTrue(repo.drones.size >= 10)
    }

    @Test
    fun addDrone_createsDroneInInitialState() {
        val initialCount = repo.drones.size
        repo.addDrone()
        assertEquals(initialCount + 1, repo.drones.size)

        val newDrone = repo.drones.last()
        assertEquals(100, newDrone.battery)
        assertEquals(0, newDrone.cargo)
        assertEquals(20, newDrone.temperature)
        assertEquals(Drone.Status.NORMAL, newDrone.status)
    }

    @Test
    fun deleteDrone_removesSpecificDrone() {
        val firstId = repo.drones.first().id
        val initialCount = repo.drones.size

        repo.deleteDrone(firstId)

        assertEquals(initialCount - 1, repo.drones.size)
        assertNull(repo.drone(firstId))
    }

    @Test
    fun deleteAllDrones_preservesEmptyList() {
        val allIds = repo.drones.map { it.id }
        allIds.forEach { repo.deleteDrone(it) }
        assertEquals(0, repo.drones.size)
    }

    @Test
    fun renameDrone_updatesDroneName() {
        val targetId = repo.drones.first().id
        val newName = "Флагман X-99"

        repo.renameDrone(targetId, newName)

        val updated = repo.drone(targetId)
        assertNotNull(updated)
        if (updated != null) {
            assertEquals(newName, updated.name)
        }
    }

    @Test
    fun collectData_modifiesOnlyTargetDrone() {
        repo.addDrone()
        val target = repo.drones.last()
        val targetId = target.id

        val otherDroneBefore = repo.drones.first()

        repo.collectData(targetId)

        val targetAfter = repo.drone(targetId)
        assertNotNull(targetAfter)
        if (targetAfter != null) {
            assertEquals(25, targetAfter.cargo)
            assertEquals(85, targetAfter.battery)
            assertEquals(35, targetAfter.temperature)
        }

        val otherDroneAfter = repo.drone(otherDroneBefore.id)
        assertEquals(otherDroneBefore, otherDroneAfter)
    }

    @Test
    fun reachDepletedState_throughNormalOperations() {
        repo.addDrone()
        val targetId = repo.drones.last().id

        repo.collectData(targetId)
        repo.collectData(targetId)
        repo.transmitData(targetId)
        repo.collectData(targetId)
        repo.transmitData(targetId)
        repo.collectData(targetId)

        val drone = repo.drone(targetId)
        assertNotNull(drone)
        if (drone != null) {
            assertEquals(10, drone.battery)
            assertEquals(Drone.Status.DEPLETED, drone.status)
        }
    }

    @Test
    fun resetDrone_resetsTargetDroneState() {
        repo.addDrone()
        val targetId = repo.drones.last().id

        repo.collectData(targetId)
        repo.resetDrone(targetId)

        val targetAfter = repo.drone(targetId)
        assertNotNull(targetAfter)
        if (targetAfter != null) {
            assertEquals(100, targetAfter.battery)
            assertEquals(0, targetAfter.cargo)
            assertEquals(20, targetAfter.temperature)
        }
    }

    @Test
    fun fetchTelemetry_returnsOnlineData_whenApiSucceeds() = kotlinx.coroutines.runBlocking {
        val firstId = repo.drones.first().id
        val telemetry = repo.fetchTelemetry(firstId, forceRefresh = true)
        assertTrue(telemetry.isOnlineData)
        assertEquals(20.0, telemetry.weather.temperature, 0.01)
    }

    @Test
    fun fetchTelemetry_returnsCachedData_whenApiFailsAfterSuccessfulCall() = kotlinx.coroutines.runBlocking {
        var shouldFail = false
        val failingApi = object : OpenMeteoApi {
            override suspend fun getWeather(latitude: Double, longitude: Double, currentWeather: Boolean): OpenMeteoResponseDto {
                if (shouldFail) throw java.io.IOException("Network unreachable")
                return OpenMeteoResponseDto(CurrentWeatherDto(22.5, 12.0, 0))
            }
        }
        val customRepo = DroneRepository(openMeteoApi = failingApi, mainContext = Dispatchers.Default)
        val firstId = customRepo.drones.first().id

        val onlineTelemetry = customRepo.fetchTelemetry(firstId, forceRefresh = true)
        assertTrue(onlineTelemetry.isOnlineData)
        assertEquals(22.5, onlineTelemetry.weather.temperature, 0.01)

        shouldFail = true
        val cachedTelemetry = customRepo.fetchTelemetry(firstId, forceRefresh = true)
        assertTrue(!cachedTelemetry.isOnlineData)
        assertEquals(22.5, cachedTelemetry.weather.temperature, 0.01)
        assertTrue(cachedTelemetry.notes.contains("Кэш"))
    }

    @Test
    fun fetchTelemetry_throwsDroneNetworkException_whenNoNetworkAndNoCache() = kotlinx.coroutines.runBlocking {
        val failingApi = object : OpenMeteoApi {
            override suspend fun getWeather(latitude: Double, longitude: Double, currentWeather: Boolean): OpenMeteoResponseDto {
                throw java.io.IOException("Connection refused")
            }
        }
        val customRepo = DroneRepository(openMeteoApi = failingApi, mainContext = Dispatchers.Default)
        val firstId = customRepo.drones.first().id

        try {
            customRepo.fetchTelemetry(firstId, forceRefresh = true)
            org.junit.Assert.fail("Expected DroneNetworkException")
        } catch (e: com.example.androidgame.data.DroneNetworkException) {
            assertEquals(com.example.androidgame.data.NetworkErrorReason.NO_NETWORK, e.reason)
            assertTrue(e.canRetry)
        }
    }

    @Test
    fun weatherDto_failsWhenRequiredFieldIsMissing() {
        val withoutWind = """{"current_weather":{"temperature":1.0,"weathercode":0}}"""
        org.junit.Assert.assertThrows(kotlinx.serialization.SerializationException::class.java) {
            kotlinx.serialization.json.Json.decodeFromString(OpenMeteoResponseDto.serializer(), withoutWind)
        }
    }
}
