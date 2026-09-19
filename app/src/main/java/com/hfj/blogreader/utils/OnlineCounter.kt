package com.hfj.blogreader.utils

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

object OnlineCounter {

    // ⚠️ آدرس Worker جدید را اینجا قرار دهید (بعد از Deploy)
    private const val WORKER_URL = "https://delicate-snowflake-onliniii.mhmdkwarkw.workers.dev"

    private const val PREF_NAME = "online_counter_prefs"
    private const val KEY_ONLINE_UUID = "online_uuid"

    // ✅ UUID جداگانه (مستقل از UserManager)
    fun getOnlineUuid(context: Context): String {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        var uuid = prefs.getString(KEY_ONLINE_UUID, null)

        if (uuid.isNullOrEmpty()) {
            uuid = UUID.randomUUID().toString()
            prefs.edit().putString(KEY_ONLINE_UUID, uuid).apply()
        }

        return uuid
    }

    // ✅ ثبت بازدید (POST /api/visit)
    suspend fun registerVisit(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            val uuid = getOnlineUuid(context)

            val url = URL("$WORKER_URL/api/visit")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("User-Agent", "Mozilla/5.0")

            val json = """{"uuid":"$uuid"}"""
            connection.outputStream.use { os ->
                os.write(json.toByteArray(Charsets.UTF_8))
                os.flush()
            }

            connection.responseCode == 200
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // ✅ دریافت تعداد آنلاین (GET /api/online)
    suspend fun getOnlineCount(): String = withContext(Dispatchers.IO) {
        try {
            val url = URL("$WORKER_URL/api/online")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.setRequestProperty("User-Agent", "Mozilla/5.0")

            if (connection.responseCode == 200) {
                val json = connection.inputStream.bufferedReader().use { it.readText() }
                val obj = JSONObject(json)
                obj.optString("online", "0")
            } else {
                "0"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "0"
        }
    }
}
