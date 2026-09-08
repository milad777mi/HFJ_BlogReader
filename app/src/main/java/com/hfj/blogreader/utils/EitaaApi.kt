package com.hfj.blogreader.utils

import com.hfj.blogreader.data.models.EitaaPost
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

object EitaaApi {

    suspend fun getLatestPost(): EitaaPost? = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://eitaa.com/api777")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.setRequestProperty("User-Agent", "Mozilla/5.0")

            if (connection.responseCode == 200) {
                val html = connection.inputStream.bufferedReader().use { it.readText() }

                // ✅ استخراج آخرین پیام (غیر از پیام‌های سنجاق‌شده)
                // پیدا کردن آخرین پیام با کلاس js-widget_message که service_message نباشد
                val messagePattern = Regex(
                    """<div class="etme_widget_message text_not_supported_wrap js-widget_message" data-post="api777/\d+">(.*?)</div>""",
                    RegexOption.DOT_MATCHES_ALL
                )
                val allMessages = messagePattern.findAll(html).toList()

                // آخرین پیام (آخرین عنصر در لیست)
                val lastMessage = allMessages.lastOrNull()?.groupValues?.get(1) ?: return@withContext null

                // ✅ استخراج متن (بدون تگ‌های HTML)
                val textPattern = Regex("""<div class="etme_widget_message_text js-message_text"[^>]*>(.*?)</div>""", RegexOption.DOT_MATCHES_ALL)
                val textMatch = textPattern.find(lastMessage)
                var text = textMatch?.groupValues?.get(1)?.trim() ?: ""

                // حذف تگ‌های HTML از متن
                text = text.replace(Regex("<[^>]*>"), "").trim()

                // ✅ استخراج لینک از متن (اگر وجود داشته باشد)
                val linkPattern = Regex("""<a href="([^"]+)"[^>]*>""")
                val linkMatch = linkPattern.find(lastMessage)
                val link = linkMatch?.groupValues?.get(1)

                // اگر متن خالی بود، null برگردان
                if (text.isBlank()) {
                    return@withContext null
                }

                EitaaPost(
                    text = text,
                    link = link
                )
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
