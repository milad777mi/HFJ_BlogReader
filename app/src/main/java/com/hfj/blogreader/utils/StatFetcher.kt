package com.hfj.blogreader.utils

import android.content.Context
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

                val jsCode = """
                    (function() {
                        var text = document.body.innerText;
                        // حذف فضاهای اضافی
                        text = text.replace(/\s+/g, ' ');
                        function extract(keyword) {
                            var regex = new RegExp(keyword + '\\s*[:]?\\s*(\\d+)');
                            var match = text.match(regex);
                            return match ? match[1] : '0';
                        }
                        return JSON.stringify({
                            today: extract('بازديد امروز'),
                            yesterday: extract('بازديد دیروز'),
                            weekly: extract('بازديد هفتگی'),
                            monthly: extract('بازديد ماهانه'),
                            total: extract('بازديد كل')
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
            }
        }

        webView.loadUrl("https://hfgapi77777.blogfa.com/")
        deferred.await()
    }
}
