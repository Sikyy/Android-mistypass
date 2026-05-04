package com.mistyislet.app.core.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

interface TokenStore {
    var accessToken: String?
    var refreshToken: String?
    var expiresAt: Long
    fun clear()
    fun isValid(): Boolean
}

@Singleton
class EncryptedTokenStore @Inject constructor(
    @ApplicationContext context: Context,
) : TokenStore {
    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "mistyislet_auth",
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    override var accessToken: String?
        get() = prefs.getString("access_token", null)
        set(value) = prefs.edit().putString("access_token", value).apply()

    override var refreshToken: String?
        get() = prefs.getString("refresh_token", null)
        set(value) = prefs.edit().putString("refresh_token", value).apply()

    override var expiresAt: Long
        get() = prefs.getLong("expires_at", 0L)
        set(value) = prefs.edit().putLong("expires_at", value).apply()

    override fun clear() = prefs.edit().clear().apply()

    override fun isValid(): Boolean = accessToken != null && System.currentTimeMillis() < expiresAt - 60_000
}

@Module
@InstallIn(SingletonComponent::class)
abstract class TokenStoreModule {
    @Binds
    @Singleton
    abstract fun bindTokenStore(impl: EncryptedTokenStore): TokenStore
}
