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
                // ✅ حذف اینتر: متن مستقیم (همان‌طور که در HTML است)
                val text = contentDiv?.text()?.trim() ?: ""

                // ✅ استخراج تصاویر
                val imageUrls = contentDiv?.select("img")?.mapNotNull { img ->
                    img.attr("src").takeIf { it.isNotEmpty() && it.startsWith("http") }
                } ?: emptyList()

                // ✅ استخراج فیلم
                val videoTag = contentDiv?.select("video")?.first()
                val videoUrl = videoTag?.attr("src")?.takeIf { it.isNotEmpty() }

                // ✅ استخراج تاریخ (فقط تاریخ، بدون "نوشته شده در")
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

    // ✅ استخراج تاریخ (بدون عبارت "نوشته شده در")
    private fun extractFullDate(text: String): String {
        val pattern = Regex("""نوشته شده در تاريخ (.*?) توسط""")
        val match = pattern.find(text)
        val datePart = match?.groupValues?.get(1)?.trim()
        return if (!datePart.isNullOrEmpty()) {
            datePart  // ← فقط تاریخ، مانند: جمعه سیزدهم شهریور ۱۴۰۵ ساعت 23:10
        } else {
            val fallback = text.replace("نوشته شده در تاريخ ", "").replace(" توسط.*$".toRegex(), "").trim()
            if (fallback.isNotEmpty()) fallback else "تاریخ نامشخص"
        }
    }
}
