package com.mistyislet.app.data.api

import com.mistyislet.app.domain.model.AccessLog
import com.mistyislet.app.domain.model.AccessibleDoor
import com.mistyislet.app.domain.model.BleTokenResponse
import com.mistyislet.app.domain.model.PinCodeResponse
import com.mistyislet.app.domain.model.CreateVisitorPassRequest
import com.mistyislet.app.domain.model.ListResponse
import com.mistyislet.app.domain.model.QRTokenRequest
import com.mistyislet.app.domain.model.QRTokenResponse
import com.mistyislet.app.domain.model.QRUnlockRequest
import com.mistyislet.app.domain.model.UnlockRequest
import com.mistyislet.app.domain.model.UnlockResponse
import com.mistyislet.app.domain.model.VisitorPass
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface AccessApi {

    @GET(MobileApiRoutes.fetchAppAccessMyDoorsRetrofitPath)
    suspend fun getMyDoors(): ListResponse<AccessibleDoor>

    @POST(MobileApiRoutes.appUnlockDoorRetrofitPath)
    suspend fun unlock(@Body request: UnlockRequest): UnlockResponse

    @POST(MobileApiRoutes.appQRUnlockDoorRetrofitPath)
    suspend fun qrUnlock(@Body request: QRUnlockRequest): UnlockResponse

    @GET(MobileApiRoutes.fetchAppAccessBLETokenRetrofitPath)
    suspend fun getBleToken(): BleTokenResponse

    @POST(MobileApiRoutes.postAppQrTokenRetrofitPath)
    suspend fun getQrToken(@Body request: QRTokenRequest = QRTokenRequest()): QRTokenResponse

    @GET(MobileApiRoutes.getAppAccessPinCodeRetrofitPath)
    suspend fun getPinCode(): PinCodeResponse

    @GET(MobileApiRoutes.fetchAppAccessLogsRetrofitPath)
    suspend fun getAccessLogs(
        @Query("offset") offset: Int = 0,
        @Query("limit") limit: Int = 20,
    ): ListResponse<AccessLog>

    @GET("app/visitor-passes")
    suspend fun getVisitorPasses(
        @Query("offset") offset: Int = 0,
        @Query("limit") limit: Int = 50,
    ): ListResponse<VisitorPass>

    @POST("app/visitor-passes")
    suspend fun createVisitorPass(@Body request: CreateVisitorPassRequest): VisitorPass
}
