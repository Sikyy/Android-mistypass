package com.mistyislet.app.ui.credentials

import com.mistyislet.app.core.network.ApiResult
import com.mistyislet.app.data.api.CredentialApi
import com.mistyislet.app.data.dao.CachedCredential
import com.mistyislet.app.data.dao.CredentialDao
import com.mistyislet.app.data.repository.CredentialRepository
import com.mistyislet.app.domain.model.BindNFCCardRequest
import com.mistyislet.app.domain.model.Credential
import com.mistyislet.app.domain.model.ListResponse
import com.mistyislet.app.domain.model.NFCCard
import com.mistyislet.app.domain.model.UnbindNFCCardResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BindCardViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepo: FakeCredentialRepository
    private lateinit var viewModel: BindCardViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeRepo = FakeCredentialRepository()
        viewModel = BindCardViewModel(fakeRepo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `bindCard forwards uid and exposes success`() = runTest {
        fakeRepo.bindResult = ApiResult.Success(NFCCard(id = "card-1", cardUid = "04AABBCC", isActive = true))

        viewModel.bindCard("04AABBCC")
        advanceUntilIdle()

        assertEquals("04AABBCC", fakeRepo.lastCardUid)
        assertEquals("card-1", viewModel.uiState.value.boundCard?.id)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `bindCard exposes backend error`() = runTest {
        fakeRepo.bindResult = ApiResult.Error(409, "card already bound to another user", "conflict")

        viewModel.bindCard("04AABBCC")
        advanceUntilIdle()

        assertEquals("card already bound to another user", viewModel.uiState.value.errorMessage)
        assertNull(viewModel.uiState.value.boundCard)
    }
}

private class FakeCredentialRepository : CredentialRepository(
    credentialApi = object : CredentialApi {
        override suspend fun getCredentials(): ListResponse<Credential> = ListResponse(emptyList())
        override suspend fun listNFCCards(): List<NFCCard> = emptyList()
        override suspend fun bindNFCCard(request: BindNFCCardRequest): NFCCard = NFCCard(id = "unused")
        override suspend fun unbindNFCCard(credentialId: String): UnbindNFCCardResponse =
            UnbindNFCCardResponse(status = "revoked")
    },
    credentialDao = object : CredentialDao {
        override fun getAll(): Flow<List<CachedCredential>> = flowOf(emptyList())
        override suspend fun insertAll(credentials: List<CachedCredential>) {}
        override suspend fun deleteAll() {}
    },
) {
    var bindResult: ApiResult<NFCCard> = ApiResult.Error(0, "not set")
    var lastCardUid: String? = null

    override suspend fun bindNFCCard(cardUid: String, cardType: String, label: String): ApiResult<NFCCard> {
        lastCardUid = cardUid
        return bindResult
    }
}
