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

// ✅ config کامل (شامل cacheVersion)
data class HfjConfig(
    val appOpenMs: Long,
    val refreshMs: Long,
    val loadMoreMs: Long,
    val cacheVersion: Int
)

// ✅ نتیجه‌ی صفحه اول (مطالب + صفحه بعد + config)
data class FirstPageResult(
    val posts: List<Post>,
    val nextUrl: String?,
    val config: HfjConfig?
)

class BlogRepository(private val context: Context) {

    private val baseUrl = "https://hfgapi77777.blogfa.com/"
    
    private val db = AppDatabase.getInstance(context)
    private val postDao = db.postDao()

    private val CACHE_DURATION_MS = 10 * 60 * 1000L

    // ============================================================
    // ✅ صفحه اول + config با هم (یک درخواست)
    // ============================================================
    suspend fun fetchFirstPageWithConfig(): FirstPageResult = withContext(Dispatchers.IO) {
        val doc = fetchDocument(baseUrl)
        val posts = extractPosts(doc)
        val nextLink = doc.select("a.nextlink").first()?.attr("href")
            ?.let { buildNextUrl(it) }

        // ✅ استخراج config از همون HTML
        val config = extractConfigFromDoc(doc)

        if (posts.isNotEmpty()) {
            postDao.insertPosts(posts.map { PostEntity.fromPost(it) })
        }

        FirstPageResult(posts, nextLink, config)
    }

    // ============================================================
    // ✅ استخراج config از Document
    //   ۱. اول meta tag (Worker)
    //   ۲. اگه نبود، متغیر JS (بلاگفا)
    // ============================================================
    private fun extractConfigFromDoc(doc: Document): HfjConfig? {
        // ۱. meta tag (Worker)
        try {
            val meta = doc.select("meta[name=hfj-config]").first()
            if (meta != null) {
                val json = meta.attr("content")
                if (json.isNotBlank()) {
                    parseConfigFromJson(json)?.let { return it }
                }
            }
        } catch (e: Exception) {
            // ادامه بده به چک دوم
        }

        // ۲. متغیر JS (بلاگفا): var hfj_config = { ... };
        try {
            doc.select("script").forEach { script ->
                val text = script.data()
                if (text.contains("hfj_config")) {
                    parseConfigFromJsVariable(text)?.let { return it }
                }
            }
        } catch (e: Exception) {
            // هیچی
        }

        return null
    }

    // ✅ پارس JSON (برای meta tag و متن JS)
    private fun parseConfigFromJson(json: String): HfjConfig? {
        return try {
            val appOpen  = extractLongFromJson(json, "appOpenMs")
            val refresh  = extractLongFromJson(json, "refreshMs")
            val loadMore = extractLongFromJson(json, "loadMoreMs")
            val cacheVer = extractLongFromJson(json, "cacheVersion")

            if (appOpen >= 0 && refresh >= 0 && loadMore >= 0) {
                HfjConfig(
                    appOpenMs = appOpen,
                    refreshMs = refresh,
                    loadMoreMs = loadMore,
                    cacheVersion = if (cacheVer > 0) cacheVer.toInt() else 1
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    // ✅ پارس متغیر JS: var hfj_config = { ... };
    private fun parseConfigFromJsVariable(text: String): HfjConfig? {
        return try {
            val match = Regex("""hfj_config\s*=\s*(\{[^}]+\})""").find(text) ?: return null
            parseConfigFromJson(match.groupValues[1])
        } catch (e: Exception) {
            null
        }
    }

    // ============================================================
    // ✅ Fallback: خوندن config با درخواست جدا
    // ============================================================
    suspend fun fetchConfig(): HfjConfig? = withContext(Dispatchers.IO) {
        try {
            val doc = Jsoup.connect(baseUrl)
                .timeout(10000)
                .userAgent("Mozilla/5.0")
                .ignoreContentType(true)
                .ignoreHttpErrors(true)
                .get()

            extractConfigFromDoc(doc)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // ✅ استخراج عدد (Long) از JSON ساده
    private fun extractLongFromJson(json: String, key: String): Long {
        val pattern = "\"$key\"\\s*:\\s*(\\d+)".toRegex()
        val match = pattern.find(json) ?: return -1L
        return match.groupValues[1].toLongOrNull() ?: -1L
    }

    suspend fun isCacheValid(): Boolean {
        val lastCacheTime = postDao.getLatestCacheTime() ?: return false
        val elapsed = System.currentTimeMillis() - lastCacheTime
        return elapsed < CACHE_DURATION_MS
    }

    suspend fun getCachedPosts(): List<Post> = withContext(Dispatchers.IO) {
        postDao.getAllPosts().map { it.toPost() }
    }

    // ✅ حفظ شد برای سازگاری
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
