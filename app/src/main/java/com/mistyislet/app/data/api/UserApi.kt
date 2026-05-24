package com.mistyislet.app.data.api

import com.mistyislet.app.domain.model.ChangePasswordRequest
import com.mistyislet.app.domain.model.UserInfo
import com.mistyislet.app.domain.model.UserLogin
import com.mistyislet.app.domain.model.UserLoginListResponse
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

@kotlinx.serialization.Serializable
data class SetPrimaryDeviceResponse(
    val status: String = "",
    @kotlinx.serialization.SerialName("device_id") val deviceId: String = "",
)

interface UserApi {

    @GET(MobileApiRoutes.fetchAppCurrentUserRetrofitPath)
    suspend fun getCurrentUser(): UserInfo

    @GET(MobileApiRoutes.getAppMeLoginsRetrofitPath)
    suspend fun getMyLogins(): UserLoginListResponse

    @DELETE(MobileApiRoutes.deleteAppMeLoginsLoginIdRetrofitPath)
    suspend fun remoteLogout(@Path("loginId") loginId: String)

    @POST(MobileApiRoutes.postAppMeChangePasswordRetrofitPath)
    suspend fun changePassword(@Body request: ChangePasswordRequest)

    @Multipart
    @POST(MobileApiRoutes.postAppMeAvatarRetrofitPath)
    suspend fun uploadAvatar(@Part file: MultipartBody.Part): UserInfo

    @POST(MobileApiRoutes.postAppMePrimaryDeviceRetrofitPath)
    suspend fun setPrimaryDevice(): SetPrimaryDeviceResponse
}
