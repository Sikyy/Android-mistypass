package com.mistyislet.app.ui.admin

import com.mistyislet.app.domain.model.AccessRight
import org.junit.Assert.assertEquals
import org.junit.Test

class ShareAccessTest {

    private fun right(doorId: String, canAccess: Boolean = true) =
        AccessRight(doorId = doorId, canAccess = canAccess)

    @Test
    fun `returns door ids for accessible doors`() {
        val rights = listOf(right("door-1"), right("door-2"))

        assertEquals(listOf("door-1", "door-2"), rights.shareableDoorIds())
    }

    @Test
    fun `excludes doors the user cannot access`() {
        val rights = listOf(right("door-1"), right("door-2", canAccess = false))

        assertEquals(listOf("door-1"), rights.shareableDoorIds())
    }

    @Test
    fun `deduplicates repeated door ids`() {
        // A door can appear twice (e.g. granted via both group and role).
        val rights = listOf(right("door-1"), right("door-1"))

        assertEquals(listOf("door-1"), rights.shareableDoorIds())
    }

    @Test
    fun `drops blank door ids`() {
        val rights = listOf(right("door-1"), right(""))

        assertEquals(listOf("door-1"), rights.shareableDoorIds())
    }

    @Test
    fun `returns empty list when no doors are accessible`() {
        val rights = listOf(right("door-1", canAccess = false))

        assertEquals(emptyList<String>(), rights.shareableDoorIds())
    }
}
