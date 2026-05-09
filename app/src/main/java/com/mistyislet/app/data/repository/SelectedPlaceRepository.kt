package com.mistyislet.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class SelectedScope(
    val orgId: String? = null,
    val orgName: String? = null,
    val placeId: String? = null,
    val placeName: String? = null,
)

@Singleton
class SelectedPlaceRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    val scope: Flow<SelectedScope> = dataStore.data.map { prefs ->
        SelectedScope(
            orgId = prefs[KEY_ORG_ID],
            orgName = prefs[KEY_ORG_NAME],
            placeId = prefs[KEY_PLACE_ID],
            placeName = prefs[KEY_PLACE_NAME],
        )
    }

    suspend fun selectOrg(id: String, name: String) {
        dataStore.edit {
            it[KEY_ORG_ID] = id
            it[KEY_ORG_NAME] = name
            it.remove(KEY_PLACE_ID)
            it.remove(KEY_PLACE_NAME)
        }
    }

    suspend fun clearOrg() {
        dataStore.edit {
            it.remove(KEY_ORG_ID)
            it.remove(KEY_ORG_NAME)
            it.remove(KEY_PLACE_ID)
            it.remove(KEY_PLACE_NAME)
        }
    }

    suspend fun selectPlace(id: String, name: String) {
        dataStore.edit {
            it[KEY_PLACE_ID] = id
            it[KEY_PLACE_NAME] = name
        }
    }

    suspend fun clearPlace() {
        dataStore.edit {
            it.remove(KEY_PLACE_ID)
            it.remove(KEY_PLACE_NAME)
        }
    }

    private companion object {
        val KEY_ORG_ID = stringPreferencesKey("selected_org_id")
        val KEY_ORG_NAME = stringPreferencesKey("selected_org_name")
        val KEY_PLACE_ID = stringPreferencesKey("selected_place_id")
        val KEY_PLACE_NAME = stringPreferencesKey("selected_place_name")
    }
}
