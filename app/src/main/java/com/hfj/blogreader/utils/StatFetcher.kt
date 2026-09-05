package com.hfj.blogreader.utils

import android.content.Context
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebSettings
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

        // ✅ تنظیمات کامل WebView مثل مرورگر
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            // ✅ User-Agent شبیه مرورگر واقعی
            userAgentString = "Mozilla/5.0 (Linux; Android 11; 21061119AG) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
            cacheMode = WebSettings.LOAD_NO_CACHE
            setSupportZoom(false)
            builtInZoomControls = false
            displayZoomControls = false
            allowFileAccess = true
            allowContentAccess = true
        }

        // ✅ کوکی‌ها را فعال کن
        android.webkit.CookieManager.getInstance().setAcceptCookie(true)

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)

                // ⏳ ۴ ثانیه صبر برای اجرای کامل جاوااسکریپت
                view?.postDelayed({
                    val jsCode = """
                        (function() {
                            var text = document.body.innerText;
                            text = text.replace(/\s+/g, ' ');

                            function extract(keyword) {
                                var regex = new RegExp(keyword + '\\s*[:]?\\s*(\\d+)');
                                var match = text.match(regex);
                                return match ? match[1] : '0';
                            }

                            var today = extract('بازديد امروز');
                            var yesterday = extract('بازديد دیروز');
                            var weekly = extract('بازديد هفتگی');
                            var monthly = extract('بازديد ماهانه');
                            var total = extract('بازديد كل');

                            // اگر پیدا نشد، با املاهای جایگزین امتحان کن
                            if (today == '0') today = extract('بازدید امروز');
                            if (yesterday == '0') yesterday = extract('بازدید دیروز');
                            if (weekly == '0') weekly = extract('بازدید هفتگی');
                            if (monthly == '0') monthly = extract('بازدید ماهانه');
                            if (total == '0') total = extract('بازدید كل');

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
                }, 4000) // ⏳ ۴ ثانیه صبر
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
            // ✅ تایم‌اوت ۴۵ ثانیه (چون اسکریپت خارجی است)
            withTimeout(45000L) {
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
