package com.hfj.blogreader.utils

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object LikeManager {

    private const val WORKER_URL = "https://miladhfgbnmbnn.mhmdkwarkw.workers.dev"

    suspend fun getLikeCount(postId: String): Int = withContext(Dispatchers.IO) {
        try {
            val url = URL("$WORKER_URL/api/likes?postId=$postId")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            if (connection.responseCode == 200) {
                val json = connection.inputStream.bufferedReader().use { it.readText() }
                val obj = JSONObject(json)
                obj.optInt("count", 0)
            } else {
                0
            }
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }

    suspend fun getLikeStatus(postId: String, userId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("$WORKER_URL/api/likes/status?postId=$postId&userId=$userId")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            if (connection.responseCode == 200) {
                val json = connection.inputStream.bufferedReader().use { it.readText() }
                val obj = JSONObject(json)
                obj.optBoolean("liked", false)
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun likePost(postId: String, userId: String): Int = withContext(Dispatchers.IO) {
        try {
            val url = URL("$WORKER_URL/api/likes")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.doOutput = true

            val body = JSONObject().apply {
                put("postId", postId)
                put("userId", userId)
            }

            connection.outputStream.write(body.toString().toByteArray())

            if (connection.responseCode == 200) {
                val json = connection.inputStream.bufferedReader().use { it.readText() }
                val obj = JSONObject(json)
                obj.optInt("count", 0)
            } else if (connection.responseCode == 409) {
                0
            } else {
                0
            }
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }
}
