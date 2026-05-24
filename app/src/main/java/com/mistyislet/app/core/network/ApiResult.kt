package com.mistyislet.app.core.network

import com.mistyislet.app.domain.model.ApiError
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import retrofit2.HttpException

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val code: Int, val message: String, val errorCode: String? = null) : ApiResult<Nothing>()
    data class Exception(val throwable: Throwable) : ApiResult<Nothing>()
}

private val errorJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
}

suspend fun <T> safeApiCall(block: suspend () -> T): ApiResult<T> {
    return try {
        ApiResult.Success(block())
    } catch (e: HttpException) {
        val rawBody = e.response()?.errorBody()?.string()
        val parsed = rawBody?.let { body ->
            runCatching { errorJson.decodeFromString<ApiError>(body) }.getOrNull()
        }
        ApiResult.Error(
            code = e.code(),
            message = parsed?.message ?: parsed?.error ?: rawBody?.takeIf { it.isNotBlank() } ?: e.message(),
            errorCode = parsed?.code,
        )
    } catch (e: SerializationException) {
        ApiResult.Error(0, "Service unavailable")
    } catch (e: kotlin.Exception) {
        ApiResult.Exception(e)
    }
}
