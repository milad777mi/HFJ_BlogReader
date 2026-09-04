package com.hfj.blogreader.utils

import android.content.Context
import android.webkit.WebView
import android.webkit.WebViewClient
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

                // ⏳ صبر می‌کنیم تا اسکریپت آمار کامل اجرا شود
                view?.postDelayed({

                    val jsCode = """
                        (function() {
                            // کل متن صفحه را بگیر
                            var text = document.body.innerText;
                            text = text.replace(/\s+/g, ' ');

                            function extractNumber(keyword) {
                                // جستجوی دقیق با عبارت کلیدی
                                var regex = new RegExp(keyword + '\\s*[:]?\\s*(\\d+)');
                                var match = text.match(regex);
                                return match ? match[1] : '0';
                            }

                            // ✅ استخراج با کلمات دقیق از کد شما
                            var today = extractNumber('بازديد امروز');
                            var yesterday = extractNumber('بازديد دیروز');
                            var weekly = extractNumber('بازديد هفتگی');
                            var monthly = extractNumber('بازديد ماهانه');
                            var total = extractNumber('بازديد كل');

                            // اگر چیزی پیدا نشد، با املای جایگزین امتحان کن
                            if (today == '0') today = extractNumber('بازدید امروز');
                            if (yesterday == '0') yesterday = extractNumber('بازدید دیروز');
                            if (weekly == '0') weekly = extractNumber('بازدید هفتگی');
                            if (monthly == '0') monthly = extractNumber('بازدید ماهانه');
                            if (total == '0') total = extractNumber('بازدید كل');

                            // اگر باز هم هیچ‌کدام پیدا نشد، آخرین ۵ عدد صفحه را بگیر
                            if (today == '0' && yesterday == '0' && weekly == '0' && monthly == '0' && total == '0') {
                                var allNumbers = text.match(/\d+/g);
                                if (allNumbers && allNumbers.length >= 5) {
                                    var len = allNumbers.length;
                                    today = allNumbers[len - 5] || '0';
                                    yesterday = allNumbers[len - 4] || '0';
                                    weekly = allNumbers[len - 3] || '0';
                                    monthly = allNumbers[len - 2] || '0';
                                    total = allNumbers[len - 1] || '0';
                                }
                            }

                            return JSON.stringify({
                                today: today,
                                yesterday: yesterday,
                                weekly: weekly,
                                monthly: monthly,
                                total: total
                            });
                        })();
                    """.trimIndent()

                    view?.evaluateJavascript(jsCode) { resultJson ->
                        val cleanJson = resultJson.trim().trim('"').replace("\\", "")
                        try {
                            val jsonObject = JSONObject(cleanJson)
                            val stats = BlogStats(
                                today = jsonObject.optString("today", "0"),
                                yesterday = jsonObject.optString("yesterday", "0"),
                                weekly = jsonObject.optString("weekly", "0"),
                                monthly = jsonObject.optString("monthly", "0"),
                                total = jsonObject.optString("total", "0")
                            )
                            deferred.complete(stats)
                        } catch (e: Exception) {
                            deferred.complete(BlogStats())
                        }
                        view?.destroy()
                    }
                }, 2000) // ⏳ ۲ ثانیه صبر (چون اسکریپت خارجی است)
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
            withTimeout(20000L) { // ⏳ ۲۰ ثانیه تایم‌اوت
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
}
