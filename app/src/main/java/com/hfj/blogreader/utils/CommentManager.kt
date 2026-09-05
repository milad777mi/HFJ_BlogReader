package com.hfj.blogreader.utils

import android.content.Context
import com.hfj.blogreader.data.models.Comment   // ✅ این خط را اضافه کنید
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object CommentManager {

    private const val WORKER_URL = "https://miladhfgbnmbnn.mhmdkwarkw.workers.dev"

    suspend fun getApprovedComments(postId: String): List<Comment> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$WORKER_URL/api/comments?postId=$postId")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            if (connection.responseCode == 200) {
                val json = connection.inputStream.bufferedReader().use { it.readText() }
                val obj = JSONObject(json)
                val commentsArray = obj.optJSONArray("comments") ?: JSONArray()
                (0 until commentsArray.length()).map { i ->
                    val item = commentsArray.getJSONObject(i)
                    Comment(
                        id = item.getString("id"),
                        postId = item.getString("postId"),
                        userId = item.getString("userId"),
                        userName = item.optString("userName", "مستخدم"),
                        text = item.getString("text"),
                        timestamp = item.getLong("timestamp"),
                        approvedAt = item.optLong("approvedAt")
                    )
                }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun submitComment(postId: String, userId: String, userName: String, text: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("$WORKER_URL/api/comments")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.doOutput = true

            val body = JSONObject().apply {
                put("postId", postId)
                put("userId", userId)
                put("userName", userName)
                put("text", text)
            }
            connection.outputStream.write(body.toString().toByteArray())

            connection.responseCode == 200
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
