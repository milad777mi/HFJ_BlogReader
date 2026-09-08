package com.hfj.blogreader.utils

import com.hfj.blogreader.data.models.AdData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object AdManager {

    private const val WORKER_URL = "https://floral-salad-0fd3.mhmdkwarkw.workers.dev"

    suspend fun getAdData(): AdData? = withContext(Dispatchers.IO) {
        try {
            val url = URL("$WORKER_URL/api/ad")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            if (connection.responseCode == 200) {
                val json = connection.inputStream.bufferedReader().use { it.readText() }
                val obj = JSONObject(json)
                
                // ✅ بررسی وجود imageUrl به جای exists
                val imageUrl = obj.optString("imageUrl", "")
                
                if (imageUrl.isNotEmpty()) {
                    AdData(
                        exists = true,
                        imageUrl = imageUrl,
                        link = obj.optString("link", ""),
                        title = obj.optString("title", ""),
                        updatedAt = obj.optLong("updatedAt", 0)
                    )
                } else {
                    AdData(exists = false)
                }
            } else {
                AdData(exists = false)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            AdData(exists = false)
        }
    }
}
