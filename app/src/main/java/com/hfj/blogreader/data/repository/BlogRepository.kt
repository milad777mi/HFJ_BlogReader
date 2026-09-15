package com.hfj.blogreader.data.repository

import android.content.Context
import com.hfj.blogreader.data.local.AppDatabase
import com.hfj.blogreader.data.local.PostEntity
import com.hfj.blogreader.data.models.Post
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class BlogRepository(private val context: Context) {

    private val baseUrl = "https://bllosoft-glade-6b08.bnmkiio180.workers.dev/"
    
    private val db = AppDatabase.getInstance(context)
    private val postDao = db.postDao()

    private val CACHE_DURATION_MS = 10 * 60 * 1000L

    // ============================================================
    // ✅ جدید: خوندن config از meta tag صفحه اصلی
    // برمی‌گردونه: Triple(appOpenMin, refreshMin, loadMoreMin) یا null
    // ============================================================
    suspend fun fetchConfig(): Triple<Int, Int, Int>? = withContext(Dispatchers.IO) {
        try {
            val doc = Jsoup.connect(baseUrl)
                .timeout(10000)
                .userAgent("Mozilla/5.0")
                .ignoreContentType(true)
                .ignoreHttpErrors(true)
                .get()

            val meta = doc.select("meta[name=hfj-config]").first() ?: return@withContext null
            val json = meta.attr("content")
            if (json.isBlank()) return@withContext null

            val appOpen = extractIntFromJson(json, "appOpenInterval")
            val refresh = extractIntFromJson(json, "refreshInterval")
            val loadMore = extractIntFromJson(json, "loadMoreInterval")

            if (appOpen > 0 && refresh > 0 && loadMore > 0) {
                Triple(appOpen, refresh, loadMore)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // ✅ استخراج عدد از JSON ساده (بدون کتابخانه)
    private fun extractIntFromJson(json: String, key: String): Int {
        val pattern = "\"$key\"\\s*:\\s*(\\d+)".toRegex()
        val match = pattern.find(json) ?: return 0
        return match.groupValues[1].toIntOrNull() ?: 0
    }

    suspend fun isCacheValid(): Boolean {
        val lastCacheTime = postDao.getLatestCacheTime() ?: return false
        val elapsed = System.currentTimeMillis() - lastCacheTime
        return elapsed < CACHE_DURATION_MS
    }

    suspend fun getCachedPosts(): List<Post> = withContext(Dispatchers.IO) {
        postDao.getAllPosts().map { it.toPost() }
    }

    suspend fun fetchFirstPage(): Pair<List<Post>, String?> = withContext(Dispatchers.IO) {
        val doc = fetchDocument(baseUrl)
        val posts = extractPosts(doc)
        val nextLink = doc.select("a.nextlink").first()?.attr("href")
            ?.let { buildNextUrl(it) }

        if (posts.isNotEmpty()) {
            postDao.insertPosts(posts.map { PostEntity.fromPost(it) })
        }

        posts to nextLink
    }

    suspend fun fetchNextPage(nextUrl: String): Pair<List<Post>, String?> = withContext(Dispatchers.IO) {
        val doc = fetchDocument(nextUrl)
        val posts = extractPosts(doc)
        val nextLink = doc.select("a.nextlink").first()?.attr("href")
            ?.let { buildNextUrl(it) }

        if (posts.isNotEmpty()) {
            postDao.insertPosts(posts.map { PostEntity.fromPost(it) })
        }

        posts to nextLink
    }

    suspend fun fetchAllPosts(): List<Post> = withContext(Dispatchers.IO) {
        if (isCacheValid()) {
            val cached = postDao.getAllPosts()
            if (cached.isNotEmpty()) {
                return@withContext cached.map { it.toPost() }
            }
        }

        val allPosts = mutableListOf<Post>()
        var currentUrl: String? = baseUrl

        while (currentUrl != null) {
            try {
                val doc = fetchDocument(currentUrl)
                val posts = extractPosts(doc)
                allPosts.addAll(posts)

                val nextLink = doc.select("a.nextlink").first()?.attr("href")
                currentUrl = nextLink?.let { buildNextUrl(it) }

                delay(500)
            } catch (e: Exception) {
                e.printStackTrace()
                break
            }
        }

        if (allPosts.isNotEmpty()) {
            postDao.insertPosts(allPosts.map { PostEntity.fromPost(it) })
        }

        allPosts
    }

    suspend fun refreshPosts(): List<Post> = withContext(Dispatchers.IO) {
        fetchAllPosts()
    }

    suspend fun clearCache() = withContext(Dispatchers.IO) {
        postDao.clearAll()
    }

    // ---------- متدهای داخلی ----------

    private fun buildNextUrl(href: String): String {
        return when {
            href.startsWith("http://") || href.startsWith("https://") -> href
            href.startsWith("/") -> baseUrl.trimEnd('/') + href
            else -> baseUrl + href
        }
    }

    private suspend fun fetchDocument(url: String): Document = withContext(Dispatchers.IO) {
        Jsoup.connect(url)
            .timeout(30000)
            .userAgent("Mozilla/5.0")
            .ignoreContentType(true)
            .ignoreHttpErrors(true)
            .get()
    }

    private fun extractPosts(doc: Document): List<Post> {
        val posts = mutableListOf<Post>()

        doc.select(".post").forEach { postElement ->
            try {
                val link = postElement.select("h2 a").first()
                val postId = link?.attr("href")?.replace("/post/", "") ?: return@forEach
                val title = link?.text()?.trim() ?: ""

                val contentDiv = postElement.select(".postcontent").first()
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

    private fun preserveLineBreaks(html: String): String {
        var text = html
            .replace("<br>", "\n")
            .replace("<br />", "\n")
            .replace("<br/>", "\n")
            .replace("</p>", "\n")
            .replace("<p>", "")
            .replace(Regex("<[^>]*>"), "")
            .trim()

        text = text.replace(Regex("\n{2,}"), "\n")
        return text
    }

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
