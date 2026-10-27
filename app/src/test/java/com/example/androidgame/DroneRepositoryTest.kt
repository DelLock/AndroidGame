package com.example.androidgame

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DroneRepositoryTest {

    private lateinit var repo: DroneRepository

    @Before
    fun setUp() {
        repo = DroneRepository()
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
    fun collectData_modifiesOnlyTargetDrone() {
        repo.addDrone()
        val target = repo.drones.last()
        val targetId = target.id

        val otherDroneBefore = repo.drones.first()

        repo.collectData(targetId)

        val targetAfter = repo.drone(targetId)
        assertNotNull(targetAfter)
        assertEquals(25, targetAfter!!.cargo)
        assertEquals(85, targetAfter.battery)
        assertEquals(50, targetAfter.temperature)

        val otherDroneAfter = repo.drone(otherDroneBefore.id)
        assertEquals(otherDroneBefore, otherDroneAfter)
    }

    @Test
    fun resetDrone_resetsTargetDroneState() {
        repo.addDrone()
        val targetId = repo.drones.last().id

        repo.collectData(targetId)
        repo.resetDrone(targetId)

        val targetAfter = repo.drone(targetId)
        assertNotNull(targetAfter)
        assertEquals(100, targetAfter!!.battery)
        assertEquals(0, targetAfter.cargo)
        assertEquals(20, targetAfter.temperature)
    }
}
