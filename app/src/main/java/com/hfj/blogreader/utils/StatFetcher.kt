package com.hfj.blogreader.utils

import android.content.Context
import android.os.Environment
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.*

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
                            text = text.replace(/\s+/g, ' ');
                            return JSON.stringify({
                                fullText: text
                            });
                        })();
                    """.trimIndent()

                    view?.evaluateJavascript(jsCode) { resultJson ->
                        val cleanJson = resultJson.trim().trim('"').replace("\\", "")
                        try {
                            val jsonObject = JSONObject(cleanJson)
                            val fullText = jsonObject.optString("fullText", "")

                            // ✅ ذخیره متن کامل در پوشه Downloads برای دیباگ
                            saveDebugText(fullText)

                            // استخراج اعداد
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
                            saveDebugText("❌ خطا در پردازش JSON: ${e.message}")
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
                saveDebugText("❌ خطا در بارگذاری صفحه: $errorCode - $description")
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
            saveDebugText("❌ تایم‌اوت ۲۵ ثانیه‌ای")
            webView.destroy()
            BlogStats()
        } catch (e: Exception) {
            saveDebugText("❌ خطای کلی: ${e.message}")
            webView.destroy()
            BlogStats()
        }
    }

    private fun extractNumber(text: String, keyword: String): String {
        val pattern = Regex("""$keyword\s*[:]?\s*(\d+)""")
        val match = pattern.find(text)
        return match?.groupValues?.get(1) ?: "0"
    }

    // ✅ ذخیره‌سازی در پوشه Downloads
    private fun saveDebugText(text: String) {
        try {
            val directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!directory.exists()) {
                directory.mkdirs()
            }
            val file = File(directory, "debug_stats.txt")
            val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val fullText = """
                =======================================
                زمان: $time
                =======================================
                $text
                =======================================
            """.trimIndent()
            FileWriter(file, false).use { writer ->
                PrintWriter(writer).use { printWriter ->
                    printWriter.println(fullText)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
