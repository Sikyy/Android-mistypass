package com.mistyislet.app.ui.visitors

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the visitor-pass delivery_method contract on the client side.
 *
 * The backend (`normalizeDeliveryMethod` in
 * `api/internal/modules/access/service_policies.go`) accepts only "wallet" and
 * "email_qr" and returns HTTP 400 for anything else. The create-visitor picker
 * must therefore only offer values from that set. Historically it defaulted to
 * "whatsapp", which the backend rejected on every create.
 */
class CreateVisitorDeliveryMethodTest {

    private val backendSupported = setOf("wallet", "email_qr")

    @Test
    fun pickerOffersOnlyBackendSupportedMethods() {
        assertTrue("expected at least one delivery method", supportedDeliveryMethodKeys.isNotEmpty())
        val unsupported = supportedDeliveryMethodKeys.filterNot { it in backendSupported }
        assertTrue(
            "delivery methods not accepted by the backend (would be rejected): $unsupported",
            unsupported.isEmpty(),
        )
    }

    @Test
    fun defaultDeliveryMethodIsOfferedAndSupported() {
        assertTrue(
            "default \"$defaultDeliveryMethod\" must be accepted by the backend",
            defaultDeliveryMethod in backendSupported,
        )
        assertTrue(
            "default \"$defaultDeliveryMethod\" must be one of the offered options",
            defaultDeliveryMethod in supportedDeliveryMethodKeys,
        )
    }
}
