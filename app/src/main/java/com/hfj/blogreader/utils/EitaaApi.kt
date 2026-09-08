package com.hfj.blogreader.utils

import com.hfj.blogreader.data.models.EitaaPost
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

object EitaaApi {

    suspend fun getLatestPost(): EitaaPost? = withContext(Dispatchers.IO) {
        try {
            val document = Jsoup.connect("https://eitaa.com/api777")
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .timeout(15000)
                .get()

            val messages = document.select(".etme_widget_message.js-widget_message")
            val lastMessage = messages.lastOrNull()

            if (lastMessage != null) {
                val textElement = lastMessage.selectFirst(".js-message_text")
                var text = textElement?.text()?.trim() ?: ""

                if (text.contains("سنجاق پیغام")) {
                    return@withContext null
                }

                val linkElement = textElement?.selectFirst("a")
                val link = linkElement?.attr("href") ?: ""

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
