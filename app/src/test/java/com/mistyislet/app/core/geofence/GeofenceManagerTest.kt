package com.mistyislet.app.core.geofence

import com.mistyislet.app.domain.model.AccessibleDoor
import org.junit.Assert.*
import org.junit.Test

class GeofenceManagerTest {

    @Test
    fun `computeGeofenceDiff adds new doors`() {
        val active = emptySet<String>()
        val doors = listOf(doorWithLocation("d1", 1.0, 2.0), doorWithLocation("d2", 3.0, 4.0))
        val diff = GeofenceManager.computeGeofenceDiff(active, doors)
        assertEquals(setOf("d1", "d2"), diff.toAdd.map { it.id }.toSet())
        assertTrue(diff.toRemove.isEmpty())
    }

    @Test
    fun `computeGeofenceDiff removes stale doors`() {
        val active = setOf("d1", "d2", "d3")
        val doors = listOf(doorWithLocation("d2", 1.0, 2.0))
        val diff = GeofenceManager.computeGeofenceDiff(active, doors)
        assertEquals(setOf("d1", "d3"), diff.toRemove)
        assertTrue(diff.toAdd.isEmpty())
    }

    @Test
    fun `computeGeofenceDiff skips doors without coordinates`() {
        val active = emptySet<String>()
        val doors = listOf(doorWithoutLocation("d1"), doorWithLocation("d2", 1.0, 2.0))
        val diff = GeofenceManager.computeGeofenceDiff(active, doors)
        assertEquals(1, diff.toAdd.size)
        assertEquals("d2", diff.toAdd.first().id)
    }

    @Test
    fun `computeGeofenceDiff caps at 100 doors`() {
        val active = emptySet<String>()
        val doors = (1..150).map { doorWithLocation("d$it", it.toDouble(), it.toDouble()) }
        val diff = GeofenceManager.computeGeofenceDiff(active, doors)
        assertEquals(100, diff.toAdd.size)
    }

    private fun doorWithLocation(id: String, lat: Double, lng: Double) = AccessibleDoor(
        id = id, name = "Door $id", buildingId = "b1", status = "online",
        gatewayStatus = "online", canUnlock = true, latitude = lat, longitude = lng,
    )

    private fun doorWithoutLocation(id: String) = AccessibleDoor(
        id = id, name = "Door $id", buildingId = "b1", status = "online",
        gatewayStatus = "online", canUnlock = true,
    )
}
