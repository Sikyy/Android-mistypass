package com.mistyislet.app.core.geofence

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.mistyislet.app.domain.model.AccessibleDoor
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class GeofenceDiff(
    val toAdd: List<AccessibleDoor>,
    val toRemove: Set<String>,
)

@Singleton
class GeofenceManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val client by lazy { LocationServices.getGeofencingClient(context) }
    private val activeGeofenceIds = mutableSetOf<String>()

    companion object {
        private const val TAG = "GeofenceManager"
        private const val RADIUS_METERS = 50f
        private const val MAX_GEOFENCES = 100

        fun computeGeofenceDiff(
            activeIds: Set<String>,
            doors: List<AccessibleDoor>,
        ): GeofenceDiff {
            val doorsWithCoords = doors.filter { it.latitude != null && it.longitude != null }
                .take(MAX_GEOFENCES)
            val newIds = doorsWithCoords.map { it.id }.toSet()
            val toAdd = doorsWithCoords.filter { it.id !in activeIds }
            val toRemove = activeIds - newIds
            return GeofenceDiff(toAdd, toRemove)
        }
    }

    fun syncGeofences(doors: List<AccessibleDoor>) {
        if (!hasLocationPermission()) {
            Log.w(TAG, "Missing location permission, skipping geofence sync")
            return
        }

        val diff = computeGeofenceDiff(activeGeofenceIds, doors)

        if (diff.toRemove.isNotEmpty()) {
            try {
                client.removeGeofences(diff.toRemove.toList())
                activeGeofenceIds.removeAll(diff.toRemove)
                Log.d(TAG, "Removed ${diff.toRemove.size} geofences")
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException removing geofences", e)
            }
        }

        if (diff.toAdd.isNotEmpty()) {
            val geofences = diff.toAdd.map { door ->
                Geofence.Builder()
                    .setRequestId(door.id)
                    .setCircularRegion(door.latitude!!, door.longitude!!, RADIUS_METERS)
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                    .build()
            }

            val request = GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofences(geofences)
                .build()

            try {
                client.addGeofences(request, geofencePendingIntent)
                    .addOnSuccessListener {
                        activeGeofenceIds.addAll(diff.toAdd.map { it.id })
                        Log.d(TAG, "Added ${diff.toAdd.size} geofences")
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Failed to add geofences", e)
                    }
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException adding geofences", e)
            }
        }
    }

    fun clearAll() {
        if (activeGeofenceIds.isNotEmpty()) {
            try {
                client.removeGeofences(geofencePendingIntent)
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException clearing geofences", e)
            }
            activeGeofenceIds.clear()
            Log.d(TAG, "Cleared all geofences")
        }
    }

    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    fun hasBackgroundLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
    }
}
