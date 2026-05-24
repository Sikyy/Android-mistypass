package com.mistyislet.app.data.api

import com.mistyislet.app.domain.model.LoginRequest
import com.mistyislet.app.domain.model.LoginResponse
import com.mistyislet.app.domain.model.MagicLinkRequest
import com.mistyislet.app.domain.model.MagicLinkResponse
import com.mistyislet.app.domain.model.OrgAuthConfig
import com.mistyislet.app.domain.model.RefreshRequest
import com.mistyislet.app.domain.model.RefreshResponse
import com.mistyislet.app.domain.model.RestorePasswordRequest
import com.mistyislet.app.domain.model.VerifyMagicLinkRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface AuthApi {

    @POST(MobileApiRoutes.createAppLoginSessionRetrofitPath)
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST(MobileApiRoutes.refreshAppLoginSessionRetrofitPath)
    suspend fun refresh(@Body request: RefreshRequest): RefreshResponse

    @POST(MobileApiRoutes.postAppAuthRestorePasswordRetrofitPath)
    suspend fun restorePassword(@Body request: RestorePasswordRequest)

    @GET(MobileApiRoutes.getAppAuthOrgLookupRetrofitPath)
    suspend fun orgLookup(@Query("domain") domain: String): OrgAuthConfig

    @POST(MobileApiRoutes.postAppAuthMagicLinkRetrofitPath)
    suspend fun requestMagicLink(@Body request: MagicLinkRequest): MagicLinkResponse

    @POST(MobileApiRoutes.postAppAuthMagicLinkVerifyRetrofitPath)
    suspend fun verifyMagicLink(@Body request: VerifyMagicLinkRequest): LoginResponse
}
