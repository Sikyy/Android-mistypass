package com.mistyislet.app.core.network

import kotlinx.serialization.SerializationException
import retrofit2.HttpException

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val code: Int, val message: String) : ApiResult<Nothing>()
    data class Exception(val throwable: Throwable) : ApiResult<Nothing>()
}

suspend fun <T> safeApiCall(block: suspend () -> T): ApiResult<T> {
    return try {
        ApiResult.Success(block())
    } catch (e: HttpException) {
        ApiResult.Error(e.code(), e.message())
    } catch (e: SerializationException) {
        ApiResult.Error(0, "Service unavailable")
    } catch (e: kotlin.Exception) {
        ApiResult.Exception(e)
    }
}
