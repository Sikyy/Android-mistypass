package com.mistyislet.app.ui.admin

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mistyislet.app.core.network.ApiResult
import com.mistyislet.app.data.api.AdminApi
import com.mistyislet.app.data.api.PlaceApi
import com.mistyislet.app.data.repository.AdminRepository
import com.mistyislet.app.data.repository.PlaceRepository
import com.mistyislet.app.data.repository.SelectedPlaceRepository
import com.mistyislet.app.domain.model.AccessibleDoor
import com.mistyislet.app.domain.model.AdminEvent
import com.mistyislet.app.domain.model.AdminUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.lang.reflect.Proxy

/**
 * A non-null [AdminApi] whose every method throws. The repository fakes below override
 * the handful of methods exercised by tests, so the real API is never called through.
 */
internal val unusedAdminApi: AdminApi = Proxy.newProxyInstance(
    AdminApi::class.java.classLoader,
    arrayOf(AdminApi::class.java),
) { _, method, _ -> throw UnsupportedOperationException("AdminApi.${method.name} should not be called in tests") } as AdminApi

internal val unusedPlaceApi: PlaceApi = Proxy.newProxyInstance(
    PlaceApi::class.java.classLoader,
    arrayOf(PlaceApi::class.java),
) { _, method, _ -> throw UnsupportedOperationException("PlaceApi.${method.name} should not be called in tests") } as PlaceApi

/** Real [SelectedPlaceRepository] backed by an in-memory DataStore that holds [placeId]. */
internal fun fakeSelectedPlaceRepository(placeId: String? = "place-1"): SelectedPlaceRepository {
    val prefs: Preferences =
        if (placeId != null) mutablePreferencesOf(stringPreferencesKey("selected_place_id") to placeId)
        else mutablePreferencesOf()
    val dataStore = object : DataStore<Preferences> {
        override val data: Flow<Preferences> = flowOf(prefs)
        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
            transform(prefs)
    }
    return SelectedPlaceRepository(dataStore)
}

internal class FakeAdminRepository : AdminRepository(unusedAdminApi) {
    var usersResult: ApiResult<List<AdminUser>> = ApiResult.Success(emptyList())
    var eventsResult: ApiResult<List<AdminEvent>> = ApiResult.Success(emptyList())

    override suspend fun getUsers(placeId: String): ApiResult<List<AdminUser>> = usersResult
    override suspend fun getEvents(placeId: String): ApiResult<List<AdminEvent>> = eventsResult
}

internal class FakePlaceRepository : PlaceRepository(unusedPlaceApi) {
    var doorsResult: ApiResult<List<AccessibleDoor>> = ApiResult.Success(emptyList())

    override suspend fun listPlaceDoors(placeId: String): ApiResult<List<AccessibleDoor>> = doorsResult
}
