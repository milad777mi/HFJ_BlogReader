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

                // ✅ متن با حفظ خطوط جدید
                val htmlContent = contentDiv?.html() ?: ""
                val text = preserveLineBreaks(htmlContent)

                // ✅ استخراج همه تصاویر
                val imageUrls = contentDiv?.select("img")?.mapNotNull { img ->
                    img.attr("src").takeIf { it.isNotEmpty() && it.startsWith("http") }
                } ?: emptyList()

                // ✅ استخراج فیلم
                val videoTag = contentDiv?.select("video")?.first()
                val videoUrl = videoTag?.attr("src")?.takeIf { it.isNotEmpty() }

                // ✅ تاریخ کامل با اعداد انگلیسی
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

    // ✅ حفظ خطوط جدید (تبدیل <br> و </p> به \n)
    private fun preserveLineBreaks(html: String): String {
        var text = html
            .replace("<br>", "\n")
            .replace("<br />", "\n")
            .replace("<br/>", "\n")
            .replace("</p>", "\n\n")
            .replace("<p>", "")
            .replace(Regex("<[^>]*>"), "")
            .trim()
        return text
    }

    // ✅ استخراج تاریخ کامل
    private fun extractFullDate(text: String): String {
        val pattern = Regex("""نوشته شده در تاريخ (.*?) توسط""")
        val match = pattern.find(text)
        val datePart = match?.groupValues?.get(1)?.trim() ?: return "تاریخ نامشخص"
        return convertPersianNumbersToEnglish(datePart)
    }

    // ✅ تبدیل اعداد فارسی به انگلیسی
    private fun convertPersianNumbersToEnglish(input: String): String {
        val persianDigits = mapOf(
            '۰' to '0', '۱' to '1', '۲' to '2', '۳' to '3', '۴' to '4',
            '۵' to '5', '۶' to '6', '۷' to '7', '۸' to '8', '۹' to '9'
        )
        return input.map { persianDigits[it] ?: it }.joinToString("")
    }
}
