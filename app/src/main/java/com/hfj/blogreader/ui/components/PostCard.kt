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
            val result = fetchPage(currentUrl)
            allPosts.addAll(result.posts)
            currentUrl = result.nextPageUrl ?: ""
            delay(500)
        }

        allPosts
    }

    private suspend fun fetchPage(url: String): PageResult = withContext(Dispatchers.IO) {
        try {
            val doc = Jsoup.connect(url).get()
            val posts = extractPosts(doc)
            val nextPageUrl = extractNextPageUrl(doc, url)

            PageResult(posts, nextPageUrl)
        } catch (e: Exception) {
            e.printStackTrace()
            PageResult(emptyList(), null)
        }
    }

    private fun extractPosts(doc: org.jsoup.nodes.Document): List<Post> {
        val posts = mutableListOf<Post>()

        doc.select(".post").forEach { postElement ->
            try {
                val link = postElement.select("h2 a").first()
                val postId = link?.attr("href")?.replace("/post/", "") ?: return@forEach
                val title = link?.text() ?: ""

                val contentDiv = postElement.select(".postcontent").first()
                val text = contentDiv?.text() ?: ""

                // استخراج تصویر
                val imgTag = contentDiv?.select("img")?.first()
                val imageUrl = imgTag?.attr("src")?.takeIf { it.isNotEmpty() && !it.startsWith("/img/") }

                // استخراج فیلم
                val videoTag = contentDiv?.select("video")?.first()
                val videoUrl = videoTag?.attr("src")?.takeIf { it.isNotEmpty() }

                // استخراج تاریخ
                val infoText = postElement.select(".postinfo").text()
                val date = extractDate(infoText)

                // استخراج هشتگ‌ها
                val hashtags = mutableListOf<String>()
                postElement.select(".posttags a").forEach { tag ->
                    hashtags.add("#${tag.text()}")
                }
                // هشتگ‌های داخل متن
                val inlineHashtags = extractHashtagsFromText(text)
                hashtags.addAll(inlineHashtags)

                posts.add(
                    Post(
                        id = postId,
                        title = title,
                        content = text,
                        imageUrl = imageUrl,
                        videoUrl = videoUrl,
                        date = date,
                        hashtags = hashtags.distinct(),
                        views = extractViews(infoText)
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return posts
    }

    private fun extractHashtagsFromText(text: String): List<String> {
        val pattern = Regex("#[\\w\\p{L}\\d_]+")
        return pattern.findAll(text).map { it.value }.toList()
    }

    private fun extractDate(text: String): String {
        val patterns = listOf(
            Regex("(\\d{2,4}[/\\-]\\d{1,2}[/\\-]\\d{1,2})"),
            Regex("(\\d{2,4}\\s+\\w+\\s+\\d{2,4})"),
            Regex("(\\w+\\s+\\d{1,2}[،,،]\\s+\\d{2,4})")
        )
        for (pattern in patterns) {
            val match = pattern.find(text)
            if (match != null) return match.value
        }
        return "تاريخ غير محدد"
    }

    private fun extractViews(text: String): String {
        val pattern = Regex("(\\d+[،,]?\\d*)\\s*(بازدید|نفر|view)")
        val match = pattern.find(text)
        return match?.groupValues?.get(1) ?: "0"
    }

    private fun extractNextPageUrl(doc: org.jsoup.nodes.Document, currentUrl: String): String? {
        val nextLink = doc.select("a.nextlink").first()
        val href = nextLink?.attr("href") ?: return null
        return if (href.startsWith("?")) "$currentUrl$href" else null
    }

    private data class PageResult(
        val posts: List<Post>,
        val nextPageUrl: String?
    )
}
