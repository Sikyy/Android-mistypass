package com.mistyislet.app.core.deeplink

import android.content.Intent
import android.net.Uri

object DeepLinkHandler {

    private const val CUSTOM_SCHEME = "mistyislet"
    private const val APP_HOST = "app.mistyislet.com"
    private const val MAGIC_LINK_HOST = "magic-link"
    private const val SSO_CALLBACK_PATH = "/sso/callback"

    fun extractMagicLinkToken(intent: Intent): String? {
        val uri = intent.data ?: return null
        if (uri.scheme == CUSTOM_SCHEME && uri.host == MAGIC_LINK_HOST) {
            return uri.getQueryParameter("token")
        }
        return null
    }

    fun extractSSOCallbackToken(intent: Intent): String? {
        val uri = intent.data ?: return null
        if (uri.scheme == "https" && uri.host == APP_HOST && uri.path == SSO_CALLBACK_PATH) {
            return uri.getQueryParameter("token")
        }
        return null
    }

    fun isMagicLinkIntent(intent: Intent): Boolean {
        val uri = intent.data ?: return false
        return uri.scheme == CUSTOM_SCHEME && uri.host == MAGIC_LINK_HOST
    }

    fun extractAuthToken(intent: Intent): String? {
        return extractMagicLinkToken(intent) ?: extractSSOCallbackToken(intent)
    }
}
