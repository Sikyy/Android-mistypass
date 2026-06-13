package com.mistyislet.app.ui.admin

import com.mistyislet.app.BuildConfig
import com.mistyislet.app.core.network.ApiResult
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Central policy + helpers for the admin console's demo/mock data fallback.
 *
 * Mock/demo builds fall back to [AdminDemoData] when the API is unavailable so the
 * console stays explorable offline. Staging/prod builds MUST surface real failures
 * instead of fabricating doors/gateways/users — see server docs
 * CODE-REVIEW-2026-06-10.md (M-1) and mobile-followups-2026-06-12.md.
 *
 * Inject this into admin ViewModels instead of branching on BuildConfig per screen,
 * so the demo gate lives in exactly one place and can't be missed on a new screen.
 */
class AdminDemoFallback(private val allowDemoData: Boolean) {

    /** Returns [demo] in mock/demo builds, or null in staging/prod so callers surface the real error. */
    fun <T> demoOrNull(demo: () -> T): T? = if (allowDemoData) demo() else null

    /**
     * Folds an [ApiResult] list into items + error for a standard admin list screen:
     *  - Success: server data; if the server list is empty, demo data only when allowed.
     *  - Error/Exception: demo data in mock/demo builds (no error), otherwise an empty
     *    list plus the real error message so the UI shows an error with a retry.
     */
    fun <T> resolveList(result: ApiResult<List<T>>, demo: () -> List<T>): AdminListResult<T> =
        when (result) {
            is ApiResult.Success ->
                AdminListResult(result.data.ifEmpty { demoOrNull(demo).orEmpty() }, null)
            is ApiResult.Error ->
                demoOrNull(demo)?.let { AdminListResult(it, null) }
                    ?: AdminListResult(emptyList(), result.message)
            is ApiResult.Exception ->
                demoOrNull(demo)?.let { AdminListResult(it, null) }
                    ?: AdminListResult(emptyList(), result.throwable.adminErrorMessage())
        }
}

data class AdminListResult<T>(val items: List<T>, val error: String?)

fun Throwable.adminErrorMessage(): String =
    localizedMessage?.takeIf { it.isNotBlank() } ?: "Network error. Pull to retry."

@Module
@InstallIn(SingletonComponent::class)
object AdminDemoModule {
    @Provides
    @Singleton
    fun provideAdminDemoFallback(): AdminDemoFallback = AdminDemoFallback(BuildConfig.ALLOW_DEMO_DATA)
}
