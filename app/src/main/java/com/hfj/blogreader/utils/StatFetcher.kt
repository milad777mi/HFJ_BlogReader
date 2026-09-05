package com.hfj.blogreader.utils

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

data class BlogStats(
    val today: String = "0",
    val yesterday: String = "0",
    val weekly: String = "0",
    val monthly: String = "0",
    val total: String = "0"
)

object StatFetcher {

    suspend fun fetchStats(context: Context): BlogStats = withContext(Dispatchers.IO) {
        try {
            // ✅ لینک مستقیم ویجت آمار (از کد HTML وبلاگ شما)
            val url = URL("https://1abzar.ir/abzar/tools/stat/amar-v3.php?color=333333&bg=F7F4D9&kc=888888&kadr=1&amar=zg6b35q745v--il2mgsu2hsciaa3vf&show=1|1|1|1|0|1|1")

            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")

            val responseCode = connection.responseCode
            if (responseCode == 200) {
                val html = connection.inputStream.bufferedReader().use { it.readText() }

                // استخراج اعداد از HTML ویجت
                val today = extractNumber(html, "بازديد امروز")
                val yesterday = extractNumber(html, "بازديد دیروز")
                val weekly = extractNumber(html, "بازديد هفتگی")
                val monthly = extractNumber(html, "بازديد ماهانه")
                val total = extractNumber(html, "بازديد كل")

                BlogStats(today, yesterday, weekly, monthly, total)
            } else {
                BlogStats()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            BlogStats()
        }
    }

    private fun extractNumber(html: String, keyword: String): String {
        val pattern = Regex("""$keyword\s*[:]?\s*(\d+)""")
        val match = pattern.find(html)
        return match?.groupValues?.get(1) ?: "0"
    }
}
