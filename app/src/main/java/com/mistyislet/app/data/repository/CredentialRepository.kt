package com.mistyislet.app.data.repository

import com.mistyislet.app.core.network.ApiResult
import com.mistyislet.app.core.network.safeApiCall
import com.mistyislet.app.data.api.CredentialApi
import com.mistyislet.app.data.dao.CachedCredential
import com.mistyislet.app.data.dao.CredentialDao
import com.mistyislet.app.domain.model.BindNFCCardRequest
import com.mistyislet.app.domain.model.Credential
import com.mistyislet.app.domain.model.NFCCard
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class CredentialRepository @Inject constructor(
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

    suspend fun listNFCCards(): ApiResult<List<NFCCard>> =
        safeApiCall { credentialApi.listNFCCards() }

    open suspend fun bindNFCCard(
        cardUid: String,
        cardType: String = "desfire_ev3",
        label: String = "NFC Card",
    ): ApiResult<NFCCard> =
        safeApiCall {
            credentialApi.bindNFCCard(
                BindNFCCardRequest(cardUid = cardUid, cardType = cardType, label = label),
            )
        }

    suspend fun unbindNFCCard(credentialId: String): ApiResult<Unit> =
        safeApiCall {
            credentialApi.unbindNFCCard(credentialId)
            Unit
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
