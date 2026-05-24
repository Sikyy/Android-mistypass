package com.mistyislet.app.data.api

import com.mistyislet.app.domain.model.RegisterDeviceRequest
import com.mistyislet.app.domain.model.RegisterDeviceResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface DeviceApi {

    @POST(MobileApiRoutes.postAppDevicesRegisterRetrofitPath)
    suspend fun registerDevice(@Body request: RegisterDeviceRequest): RegisterDeviceResponse
}
