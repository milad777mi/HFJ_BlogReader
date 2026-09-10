package com.hfj.blogreader.utils

import com.hfj.blogreader.data.models.AdTextItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object AdTextManager {

    private const val WORKER_URL = "https://bitter-shadow-5a2a.mhmdkwarkw.workers.dev"

    suspend fun getAdTextItems(): List<AdTextItem>? = withContext(Dispatchers.IO) {
        try {
            val url = URL("$WORKER_URL/api/messages")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            if (connection.responseCode == 200) {
                val json = connection.inputStream.bufferedReader().use { it.readText() }
                val obj = JSONObject(json)

                val messagesArray = obj.optJSONArray("messages") ?: return@withContext emptyList()

                val items = mutableListOf<AdTextItem>()
                for (i in 0 until messagesArray.length()) {
                    val msg = messagesArray.getJSONObject(i)
                    val text = msg.optString("text", "")
                    val link = msg.optString("link", "")
                    val id = msg.optString("id", "")

                    if (text.isNotEmpty()) {
                        items.add(
                            AdTextItem(
                                id = id,
                                text = text,
                                link = link.ifEmpty { null }
                            )
                        )
                    }
                }
                items
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
