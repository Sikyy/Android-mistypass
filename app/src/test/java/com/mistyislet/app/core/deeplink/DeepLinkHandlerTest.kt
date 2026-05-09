package com.mistyislet.app.core.deeplink

import android.content.Intent
import android.net.Uri
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DeepLinkHandlerTest {

    @Test
    fun `extractMagicLinkToken returns token from custom scheme`() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("mistyislet://magic-link?token=abc123"))
        assertEquals("abc123", DeepLinkHandler.extractMagicLinkToken(intent))
    }

    @Test
    fun `extractMagicLinkToken returns null for non-magic-link intent`() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("mistyislet://unlock/door-1"))
        assertNull(DeepLinkHandler.extractMagicLinkToken(intent))
    }

    @Test
    fun `extractMagicLinkToken returns null for null data`() {
        val intent = Intent(Intent.ACTION_MAIN)
        assertNull(DeepLinkHandler.extractMagicLinkToken(intent))
    }

    @Test
    fun `extractSSOCallbackToken returns token from https callback`() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://app.mistyislet.com/sso/callback?token=sso-jwt-xyz"))
        assertEquals("sso-jwt-xyz", DeepLinkHandler.extractSSOCallbackToken(intent))
    }

    @Test
    fun `extractSSOCallbackToken returns null for non-callback path`() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://app.mistyislet.com/visitor/abc"))
        assertNull(DeepLinkHandler.extractSSOCallbackToken(intent))
    }

    @Test
    fun `isMagicLinkIntent returns true for magic-link URI`() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("mistyislet://magic-link?token=x"))
        assertTrue(DeepLinkHandler.isMagicLinkIntent(intent))
    }

    @Test
    fun `isMagicLinkIntent returns false for regular deep link`() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("mistyislet://pass"))
        assertFalse(DeepLinkHandler.isMagicLinkIntent(intent))
    }
}
