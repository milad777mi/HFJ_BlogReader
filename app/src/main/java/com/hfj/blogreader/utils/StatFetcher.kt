package com.hfj.blogreader.utils

import android.content.Context
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

data class BlogStats(
    val today: String = "0",
    val yesterday: String = "0",
    val weekly: String = "0",
    val monthly: String = "0",
    val total: String = "0"
)

object StatFetcher {

    suspend fun fetchStats(context: Context): BlogStats = withContext(Dispatchers.Main) {
        val deferred = CompletableDeferred<BlogStats>()
        val webView = WebView(context)

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)

                // ⏳ صبر ۳ ثانیه برای اجرای کامل جاوااسکریپت
                view?.postDelayed({

                    val jsCode = """
                        (function() {
                            var text = document.body.innerText;
                            // حذف فضاهای اضافی
                            text = text.replace(/\s+/g, ' ');
                            // ✅ برای دیباگ: کل متن را برمی‌گردانیم
                            return JSON.stringify({
                                fullText: text,
                                // فقط برای تست
                                debug: 'OK'
                            });
                        })();
                    """.trimIndent()

                    view?.evaluateJavascript(jsCode) { resultJson ->
                        val cleanJson = resultJson.trim().trim('"').replace("\\", "")
                        try {
                            val jsonObject = JSONObject(cleanJson)
                            val fullText = jsonObject.optString("fullText", "")

                            // 🔍 نمایش متن واقعی در Toast برای دیباگ
                            if (fullText.isNotEmpty()) {
                                // فقط ۲۰۰ کاراکتر اول را نشان بده
                                val preview = if (fullText.length > 200) fullText.take(200) + "..." else fullText
                                Toast.makeText(context, "متن صفحه: $preview", Toast.LENGTH_LONG).show()
                            }

                            // استخراج اعداد با جستجوی مستقیم در متن
                            val today = extractNumber(fullText, "بازديد امروز")
                            val yesterday = extractNumber(fullText, "بازديد دیروز")
                            val weekly = extractNumber(fullText, "بازديد هفتگی")
                            val monthly = extractNumber(fullText, "بازديد ماهانه")
                            val total = extractNumber(fullText, "بازديد كل")

                            val stats = BlogStats(
                                today = today,
                                yesterday = yesterday,
                                weekly = weekly,
                                monthly = monthly,
                                total = total
                            )
                            deferred.complete(stats)
                        } catch (e: Exception) {
                            deferred.complete(BlogStats())
                        }
                        view?.destroy()
                    }
                }, 3000) // ⏳ ۳ ثانیه صبر
            }

            override fun onReceivedError(
                view: WebView?,
                errorCode: Int,
                description: String?,
                failingUrl: String?
            ) {
                super.onReceivedError(view, errorCode, description, failingUrl)
                deferred.complete(BlogStats())
                view?.destroy()
            }
        }

        try {
            withTimeout(25000L) {
                webView.loadUrl("https://hfgapi77777.blogfa.com/")
                deferred.await()
            }
        } catch (e: TimeoutCancellationException) {
            webView.destroy()
            BlogStats()
        } catch (e: Exception) {
            webView.destroy()
            BlogStats()
        }
    }

    // ✅ استخراج عدد با جستجوی مستقیم
    private fun extractNumber(text: String, keyword: String): String {
        val pattern = Regex("""$keyword\s*[:]?\s*(\d+)""")
        val match = pattern.find(text)
        return match?.groupValues?.get(1) ?: "0"
    }
}
