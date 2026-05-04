package com.mistyislet.app.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
)

@Serializable
data class LoginResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("expires_in") val expiresIn: Long,
    val user: UserInfo,
)

@Serializable
data class RefreshRequest(
    @SerialName("refresh_token") val refreshToken: String,
)

@Serializable
data class RefreshResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("expires_in") val expiresIn: Long,
)

@Serializable
data class UserInfo(
    val id: String,
    val email: String,
    val name: String,
    @SerialName("tenant_id") val tenantId: String,
    val language: String? = null,
)

@Serializable
data class Pagination(
    val offset: Int,
    val limit: Int,
    val total: Int,
    @SerialName("has_more") val hasMore: Boolean,
)

@Serializable
data class ListResponse<T>(
    val items: List<T>,
    val pagination: Pagination? = null,
)

@Serializable
data class ApiError(
    val error: String? = null,
    val message: String? = null,
    val code: String? = null,
    val status: String? = null,
)
