package com.shoecycle.data.featureflags

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Lenient JSON reader for feature-flag payloads. `ignoreUnknownKeys` is required so the reserved
 * `targeting` block and any future v2+ fields never crash a v1 client (spec §1.1).
 *
 * `coerceInputValues` is deliberately NOT set (ShoeCycle-Web-54b): it silently coerces a null (or
 * a type mismatch) back to the property default, which is exactly the fail-OPEN mechanism the
 * kill-switch fix warns against. Fail-closed behaviour lives in the per-field / per-flag
 * serializers on [FeatureFlag] instead, so a future field is safe by construction rather than by
 * remembering to give it a false-safe default.
 */
internal val FeatureFlagJson: Json = Json {
    ignoreUnknownKeys = true
}

/**
 * Fetches raw feature-flag definitions from the PUBLIC serve endpoint. No Authorization header is
 * ever attached — the read is intentionally unauthenticated (epic key design departure).
 */
interface FeatureFlagsService {
    /** Returns the served definitions, or throws IOException on network/parse failure. */
    suspend fun fetchFlags(): FeatureFlagsResponse
}

class FeatureFlagsServiceImpl(
    private val client: OkHttpClient = createDefaultOkHttpClient()
) : FeatureFlagsService {

    companion object {
        private const val TAG = "FeatureFlagsService"

        fun createDefaultOkHttpClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .connectTimeout(FeatureFlagConstants.REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(FeatureFlagConstants.REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .build()
        }
    }

    override suspend fun fetchFlags(): FeatureFlagsResponse = withContext(Dispatchers.IO) {
        // OkHttp's execute() is a BLOCKING call; confine it to the IO dispatcher so this suspend
        // function is honest regardless of the caller's dispatcher (ShoeCycle-Web-tk6). Matches the
        // repo convention of wrapping blocking I/O in withContext(Dispatchers.IO).
        // Deliberately NO Authorization header — the serve route is public.
        val request = Request.Builder()
            .url(FeatureFlagConstants.SERVE_URL)
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Feature-flag fetch failed: HTTP ${response.code}")
            }
            val body = response.body?.string()
                ?: throw IOException("Feature-flag fetch returned an empty body")
            Log.d(TAG, "Fetched feature-flag definitions")
            FeatureFlagJson.decodeFromString(FeatureFlagsResponse.serializer(), body)
        }
    }
}
