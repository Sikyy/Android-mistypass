package com.mistyislet.app.data.repository

import com.mistyislet.app.core.network.ApiResult
import com.mistyislet.app.core.network.safeApiCall
import com.mistyislet.app.data.api.CredentialApi
import com.mistyislet.app.data.dao.CachedCredential
import com.mistyislet.app.data.dao.CredentialDao
import com.mistyislet.app.domain.model.Credential
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CredentialRepository @Inject constructor(
    private val credentialApi: CredentialApi,
    private val credentialDao: CredentialDao,
) {
    fun getCachedCredentials(): Flow<List<Credential>> {
        return credentialDao.getAll().map { cached ->
            cached.map { it.toDomain() }
        }
    }

    suspend fun refreshCredentials(): ApiResult<List<Credential>> {
        return safeApiCall {
            val response = credentialApi.getCredentials()
            val credentials = response.items
            credentialDao.deleteAll()
            credentialDao.insertAll(credentials.map { it.toCache() })
            credentials
        }
    }
}

private fun CachedCredential.toDomain() = Credential(
    id = id,
    credentialKind = credentialKind,
    provider = provider,
    status = status,
    saveLink = saveLink,
    cardNumber = cardNumber,
)

private fun Credential.toCache() = CachedCredential(
    id = id,
    credentialKind = credentialKind,
    provider = provider,
    status = status,
    saveLink = saveLink,
    cardNumber = cardNumber,
)
