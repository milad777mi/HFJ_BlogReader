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

    // ✅ اصلاح شد: / در انتها
    private val baseUrl = "https://bllosoft-glade-6b08.bnmkiio180.workers.dev/"
    
    private val db = AppDatabase.getInstance(context)
    private val postDao = db.postDao()

    // ⏱️ مدت کش: ۱۰ دقیقه
    private val CACHE_DURATION_MS = 10 * 60 * 1000L

    /**
     * ✅ بررسی می‌کنه که کش معتبره یا نه (کمتر از ۱۰ دقیقه گذشته)
     */
    suspend fun isCacheValid(): Boolean {
        val lastCacheTime = postDao.getLatestCacheTime() ?: return false
        val elapsed = System.currentTimeMillis() - lastCacheTime
        return elapsed < CACHE_DURATION_MS
    }

    /**
     * ✅ فقط از دیتابیس محلی می‌خونه (بدون درخواست به سرور)
     */
    suspend fun getCachedPosts(): List<Post> = withContext(Dispatchers.IO) {
        postDao.getAllPosts().map { it.toPost() }
    }

    /**
     * ✅ فقط صفحه اول رو از سرور می‌گیره و توی دیتابیس ذخیره می‌کنه
     * @return لیست پست‌های صفحه اول + آدرس صفحه بعد (اگه وجود داشته باشه)
     */
    suspend fun fetchFirstPage(): Pair<List<Post>, String?> = withContext(Dispatchers.IO) {
        val doc = fetchDocument(baseUrl)
        val posts = extractPosts(doc)
        val nextLink = doc.select("a.nextlink").first()?.attr("href")
            ?.let { buildNextUrl(it) }

        // ذخیره توی دیتابیس
        if (posts.isNotEmpty()) {
            postDao.insertPosts(posts.map { PostEntity.fromPost(it) })
        }

        posts to nextLink
    }

    /**
     * ✅ صفحه بعد رو از سرور می‌گیره (برای اسکرول بی‌نهایت)
     * @param nextUrl آدرس صفحه بعد
     * @return لیست پست‌ها + آدرس صفحه بعدی
     */
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

    /**
     * ✅ API قدیمی (برای سازگاری با MainViewModel فعلی)
     */
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

    /**
     * ✅ فقط وقتی به سرور درخواست بزن که کش منقضی شده باشه
     */
    suspend fun refreshPosts(): List<Post> = withContext(Dispatchers.IO) {
        fetchAllPosts()
    }

    /**
     * ✅ پاک کردن کش (اختیاری)
     */
    suspend fun clearCache() = withContext(Dispatchers.IO) {
        postDao.clearAll()
    }

    // ---------- متدهای داخلی ----------

    /**
     * ✅ ساخت URL بعدی به صورت مطمئن
     * این تابع مطمئن می‌شه که URL درست ساخته بشه
     */
    private fun buildNextUrl(href: String): String {
        return when {
            // اگه href خودش کامل باشه (http یا https)
            href.startsWith("http://") || href.startsWith("https://") -> href
            // اگه href با / شروع بشه
            href.startsWith("/") -> baseUrl.trimEnd('/') + href
            // اگه href با ? شروع بشه (مثل ?p=2)
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
