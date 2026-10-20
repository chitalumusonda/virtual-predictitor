package com.example.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class BetPawaClient(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val baseUrl = "https://www.betpawa.co.zm"
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    /**
     * Registers a system network callback to automatically catch connection dropouts
     * in real-time and alert the repository to save the collection state immediately.
     */
    fun registerConnectivityCallback(
        onConnectionLost: () -> Unit,
        onConnectionAvailable: () -> Unit
    ) {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager?.registerNetworkCallback(
            request,
            object : ConnectivityManager.NetworkCallback() {
                override fun onLost(network: Network) {
                    onConnectionLost()
                }

                override fun onAvailable(network: Network) {
                    onConnectionAvailable()
                }
            }
        )
    }

    /**
     * Checks if physical internet connectivity is active on the Android device.
     */
    fun isNetworkAvailable(): Boolean {
        val activeNetwork = connectivityManager?.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    /**
     * Fetches current active seasons and rounds from BetPawa API.
     */
    suspend fun fetchActiveSeasons(): Result<JSONObject> = withContext(Dispatchers.IO) {
        if (!isNetworkAvailable()) {
            return@withContext Result.failure(IllegalStateException("No internet connection available"))
        }

        try {
            val request = Request.Builder()
                .url("$baseUrl/api/sportsbook/virtual/v2/seasons/list/actual")
                .header("x-pawa-brand", "betpawa-zambia")
                .header("x-pawa-language", "en")
                .header("accept", "application/json")
                .header("user-agent", "Mozilla/5.0 VirtualCollectorUltimate/1.0")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        IllegalStateException("Server responded with HTTP ${response.code}")
                    )
                }
                val body = response.body?.string() ?: ""
                Result.success(JSONObject(body))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetches events, markets, odds and scores for a specific round from BetPawa.
     */
    suspend fun fetchRoundEvents(roundId: String): Result<JSONObject> = withContext(Dispatchers.IO) {
        if (!isNetworkAvailable()) {
            return@withContext Result.failure(IllegalStateException("No internet connection available"))
        }

        try {
            val request = Request.Builder()
                .url("$baseUrl/api/sportsbook/virtual/v3/events/list/by-round/$roundId")
                .header("x-pawa-brand", "betpawa-zambia")
                .header("x-pawa-language", "en")
                .header("accept", "application/json")
                .header("user-agent", "Mozilla/5.0 VirtualCollectorUltimate/1.0")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        IllegalStateException("Server responded with HTTP ${response.code}")
                    )
                }
                val body = response.body?.string() ?: ""
                Result.success(JSONObject(body))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
