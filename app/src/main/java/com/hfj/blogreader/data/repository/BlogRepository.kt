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

    // ✅ استخراج تاریخ و حذف کلمه "ساعت"
    private fun extractFullDate(text: String): String {
        // استخراج بخش تاریخ از متن
        val pattern = Regex("""نوشته شده در تاريخ (.*?) توسط""")
        val match = pattern.find(text)
        var datePart = match?.groupValues?.get(1)?.trim() ?: ""

        // اگر الگوی بالا کار نکرد، از fallback استفاده کن
        if (datePart.isEmpty()) {
            datePart = text.replace("نوشته شده در تاريخ ", "").replace(" توسط.*$".toRegex(), "").trim()
        }

        // ✅ حذف کلمه "ساعت" و فاصله‌های اضافی
        // مثال: "جمعه سیزدهم شهریور ۱۴۰۵ ساعت 23:10" → "جمعه سیزدهم شهریور ۱۴۰۵ 23:10"
        datePart = datePart.replace(Regex("""\s*ساعت\s*"""), " ").trim()

        // اگر بیش از یک "ساعت" وجود داشت (مثل "ساعت 15:28 ساعت")، آن را هم حذف کن
        datePart = datePart.replace(Regex("""\s*ساعت\s*$"""), "").trim()

        return if (datePart.isNotEmpty()) datePart else "تاریخ نامشخص"
    }
}
