package com.hfj.blogreader

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hfj.blogreader.data.repository.BlogRepository
import kotlinx.coroutines.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var resultText by remember { mutableStateOf("⏳ منتظر کلیک...") }
            var isLoading by remember { mutableStateOf(false) }

            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(resultText, style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        isLoading = true
                        resultText = "🔄 در حال دریافت..."
                        GlobalScope.launch(Dispatchers.Main) {
                            try {
                                val repo = BlogRepository()
                                val posts = repo.fetchAllPosts()
                                if (posts.isEmpty()) {
                                    resultText = "⚠️ هیچ پستی یافت نشد"
                                    Toast.makeText(
                                        this@MainActivity,
                                        "⚠️ هیچ پستی یافت نشد",
                                        Toast.LENGTH_LONG
                                    ).show()
                                } else {
                                    resultText = "✅ ${posts.size} پست دریافت شد"
                                    Toast.makeText(
                                        this@MainActivity,
                                        "✅ ${posts.size} پست دریافت شد",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            } catch (e: Exception) {
                                resultText = "❌ خطا: ${e.message}"
                                Toast.makeText(
                                    this@MainActivity,
                                    "❌ خطا: ${e.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                                e.printStackTrace()
                            }
                            isLoading = false
                        }
                    },
                    enabled = !isLoading
                ) {
                    Text(if (isLoading) "در حال دریافت..." else "📥 دریافت پست‌ها")
                }
            }
        }
    }
}
