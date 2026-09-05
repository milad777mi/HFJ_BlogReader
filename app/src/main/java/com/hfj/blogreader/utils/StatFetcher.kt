package com.hfj.blogreader.utils

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class BlogStats(
    val today: String = "0",
    val total: String = "0"
)

object StatFetcher {

    // ✅ آدرس Worker خود را اینجا قرار دهید
    private const val WORKER_URL = "https://Miladhfgbnmbnn.workers.dev"

    suspend fun incrementAndFetchStats(context: Context): BlogStats = withContext(Dispatchers.IO) {
        try {
            val url = URL("$WORKER_URL/api/stats/increment")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.setRequestProperty("User-Agent", "Mozilla/5.0")

            val responseCode = connection.responseCode
            if (responseCode == 200) {
                val json = connection.inputStream.bufferedReader().use { it.readText() }
                val obj = JSONObject(json)
                BlogStats(
                    today = obj.optString("today", "0"),
                    total = obj.optString("total", "0")
                )
            } else {
                BlogStats()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            BlogStats()
        }
    }

    suspend fun fetchStatsOnly(): BlogStats = withContext(Dispatchers.IO) {
        try {
            val url = URL("$WORKER_URL/api/stats")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            if (connection.responseCode == 200) {
                val json = connection.inputStream.bufferedReader().use { it.readText() }
                val obj = JSONObject(json)
                BlogStats(
                    today = obj.optString("today", "0"),
                    total = obj.optString("total", "0")
                )
            } else {
                BlogStats()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            BlogStats()
        }
    }
}
