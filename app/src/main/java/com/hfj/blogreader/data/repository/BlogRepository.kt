package com.hfj.blogreader.data.repository

import com.hfj.blogreader.data.models.Post
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

class BlogRepository {

    private val baseUrl = "https://hfgapi77777.blogfa.com"

    suspend fun fetchAllPosts(): List<Post> = withContext(Dispatchers.IO) {
        val allPosts = mutableListOf<Post>()
        var currentUrl = baseUrl

        while (currentUrl.isNotEmpty()) {
            val doc = Jsoup.connect(currentUrl)
                .timeout(30000)
                .userAgent("Mozilla/5.0")
                .ignoreContentType(true)
                .ignoreHttpErrors(true)
                .get()

            val posts = extractPosts(doc)
            allPosts.addAll(posts)

            val nextLink = doc.select("a.nextlink").first()
            currentUrl = nextLink?.attr("href")?.let { "$baseUrl$it" } ?: ""
            delay(500)
        }

        allPosts
    }

    private fun extractPosts(doc: org.jsoup.nodes.Document): List<Post> {
        val posts = mutableListOf<Post>()

        doc.select(".post").forEach { postElement ->
            try {
                val link = postElement.select("h2 a").first()
                val postId = link?.attr("href")?.replace("/post/", "") ?: return@forEach
                val title = link?.text()?.trim() ?: ""

                val contentDiv = postElement.select(".postcontent").first()
                val text = contentDiv?.text()?.trim() ?: ""

                val imageUrls = contentDiv?.select("img")?.mapNotNull { img ->
                    img.attr("src").takeIf { it.isNotEmpty() && it.startsWith("http") }
                } ?: emptyList()

                val videoTag = contentDiv?.select("video")?.first()
                val videoUrl = videoTag?.attr("src")?.takeIf { it.isNotEmpty() }

                val infoText = postElement.select(".postinfo").text()
                val date = extractFullDate(infoText)

                posts.add(
                    Post(
                        id = postId,
                        title = title,
                        content = text,
                        imageUrls = imageUrls,
                        videoUrl = videoUrl,
                        date = date,
                        hashtags = emptyList(),
                        views = "0"
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return posts
    }

    // ✅ استخراج تاریخ و حذف کامل "+ نوشته شده در" و "ساعت"
    private fun extractFullDate(text: String): String {
        // 1. حذف "+ نوشته شده در " یا "نوشته شده در "
        var date = text
            .replace(Regex("""^\+?\s*نوشته شده در\s*"""), "")
            .replace(Regex("""\s*توسط.*$"""), "")
            .trim()

        // 2. حذف کلمه "ساعت" و فاصله‌های اضافی
        date = date
            .replace(Regex("""\s*ساعت\s*"""), " ")
            .replace(Regex("""\s*ساعت\s*$"""), "")
            .trim()

        // 3. اگر باز هم "ساعت" اضافی در وسط باقی ماند (مثلاً "ساعت 15:28 ساعت")
        date = date.replace(Regex("""ساعت\s*(\d+:\d+)\s*ساعت"""), "$1")

        // 4. اگر چند فاصله پشت سر هم بود، به یک فاصله تبدیل کن
        date = date.replace(Regex("""\s+"""), " ").trim()

        return if (date.isNotEmpty()) date else "تاریخ نامشخص"
    }
}
