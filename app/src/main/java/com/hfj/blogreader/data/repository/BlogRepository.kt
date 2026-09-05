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
                
                // ✅ استخراج HTML و تبدیل تگ‌ها به خط جدید
                val htmlContent = contentDiv?.html() ?: ""
                val text = preserveLineBreaks(htmlContent)

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

    // ✅ تبدیل تگ‌های HTML به خط جدید و حذف تگ‌های اضافی
    private fun preserveLineBreaks(html: String): String {
        var text = html
            .replace("<br>", "\n")
            .replace("<br />", "\n")
            .replace("<br/>", "\n")
            .replace("</p>", "\n")
            .replace("<p>", "")
            .replace(Regex("<[^>]*>"), "") // حذف بقیه تگ‌ها
            .trim()

        // تبدیل چند خط خالی پشت سر هم به یک خط
        text = text.replace(Regex("\n{2,}"), "\n")

        return text
    }

    // ✅ استخراج تاریخ و حذف "+ نوشته شده در" و "ساعت"
    private fun extractFullDate(text: String): String {
        var date = text
            .replace(Regex("""^\+?\s*نوشته شده در\s*"""), "")
            .replace(Regex("""\s*توسط.*$"""), "")
            .trim()

        date = date
            .replace(Regex("""\s*ساعت\s*"""), " ")
            .replace(Regex("""\s*ساعت\s*$"""), "")
            .trim()

        date = date.replace(Regex("""\s+"""), " ").trim()

        return if (date.isNotEmpty()) date else "تاریخ نامشخص"
    }
}
