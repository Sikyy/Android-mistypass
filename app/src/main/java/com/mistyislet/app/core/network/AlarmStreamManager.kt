package com.mistyislet.app.core.network

import com.mistyislet.app.BuildConfig
import com.mistyislet.app.core.storage.TokenStore
import com.mistyislet.app.domain.model.Alarm
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmStreamManager @Inject constructor(
    private val tokenStore: TokenStore,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    // Dedicated SSE client — longer timeouts, no body logging
    private val sseClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.MINUTES)  // SSE streams are long-lived
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    /**
     * Returns a Flow of Alarm events from the SSE stream.
     * Automatically reconnects with exponential backoff on failure.
     * The flow completes when the collector cancels (e.g., screen leaves).
     */
    fun alarmEvents(): Flow<Alarm> = callbackFlow {
        var retryDelay = INITIAL_RETRY_DELAY_MS
        var retryCount = 0

        while (isActive && retryCount < MAX_RETRIES) {
            val token = tokenStore.accessToken
            if (token == null) {
                delay(retryDelay)
                continue
            }

            val request = Request.Builder()
                .url("${BuildConfig.API_BASE_URL}app/alarms/stream")
                .header("Authorization", "Bearer $token")
                .header("Accept", "text/event-stream")
                .build()

            val factory = EventSources.createFactory(sseClient)
            var eventSource: EventSource? = null
            var connectionClosed = false

            val listener = object : EventSourceListener() {
                override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                    try {
                        val alarm = json.decodeFromString<Alarm>(data)
                        trySend(alarm)
                    } catch (_: Exception) {
                        // Malformed event — skip
                    }
                }

                override fun onOpen(eventSource: EventSource, response: Response) {
                    // Connected successfully — reset backoff
                    retryDelay = INITIAL_RETRY_DELAY_MS
                    retryCount = 0
                }

                override fun onClosed(eventSource: EventSource) {
                    connectionClosed = true
                }

                override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                    connectionClosed = true
                }
            }

            eventSource = factory.newEventSource(request, listener)

            // Wait until connection closes or flow is cancelled
            while (isActive && !connectionClosed) {
                delay(1000)
            }

            eventSource.cancel()

            if (!isActive) break

            // Exponential backoff
            retryCount++
            delay(retryDelay)
            retryDelay = (retryDelay * 2).coerceAtMost(MAX_RETRY_DELAY_MS)
        }

        awaitClose { /* eventSource already cancelled above */ }
    }

    companion object {
        private const val INITIAL_RETRY_DELAY_MS = 2_000L
        private const val MAX_RETRY_DELAY_MS = 60_000L
        private const val MAX_RETRIES = 10
    }
}
