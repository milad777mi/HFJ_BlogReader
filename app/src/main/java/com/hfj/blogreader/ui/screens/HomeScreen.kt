package com.hfj.blogreader.ui.screens

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.hfj.blogreader.ui.components.PostCard
import com.hfj.blogreader.ui.components.AdBanner
import com.hfj.blogreader.ui.components.EitaaBanner
import com.hfj.blogreader.ui.theme.LocalFontScale
import com.hfj.blogreader.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    navController: NavController
) {
    val context = LocalContext.current
    val posts by viewModel.filteredPosts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val fontScale = LocalFontScale.current

    // ✅ دریافت داده‌های تبلیغاتی
    val adData by viewModel.adData.collectAsState()
    val eitaaPost by viewModel.eitaaPost.collectAsState()

    // ✅ کپی محلی برای Smart cast
    val ad = adData
    val eitaa = eitaaPost

    // ✅ دیباگ: نمایش وضعیت داده‌ها در Logcat
    LaunchedEffect(adData, eitaaPost) {
        Log.d("HomeScreen", "========== دیباگ تبلیغات ==========")
        Log.d("HomeScreen", "adData: $adData")
        Log.d("HomeScreen", "adData?.imageUrl: ${adData?.imageUrl}")
        Log.d("HomeScreen", "adData?.exists: ${adData?.exists}")
        Log.d("HomeScreen", "eitaaPost: $eitaaPost")
        Log.d("HomeScreen", "eitaaPost?.text: ${eitaaPost?.text}")
        Log.d("HomeScreen", "=====================================")
        
        // اگه adData نال بود، لاگ جداگانه
        if (adData == null) {
            Log.e("HomeScreen", "❌ adData is NULL! تبلیغات دریافت نشد.")
        } else if (adData.imageUrl.isNullOrEmpty()) {
            Log.e("HomeScreen", "❌ adData.imageUrl is NULL or EMPTY! لینک عکس وجود ندارد.")
        } else {
            Log.d("HomeScreen", "✅ adData.imageUrl: ${adData.imageUrl}")
        }
        
        if (eitaaPost == null) {
            Log.e("HomeScreen", "❌ eitaaPost is NULL! متن ایتا دریافت نشد.")
        } else if (eitaaPost.text.isNullOrBlank()) {
            Log.e("HomeScreen", "❌ eitaaPost.text is NULL or EMPTY! متن ایتا وجود ندارد.")
        } else {
            Log.d("HomeScreen", "✅ eitaaPost.text: ${eitaaPost.text}")
        }
    }

    // ✅ بارگذاری خودکار هنگام ورود
    LaunchedEffect(Unit) {
        Log.d("HomeScreen", "🔄 شروع بارگذاری تبلیغات...")
        viewModel.loadAdData()
        viewModel.loadEitaaPost()
        Log.d("HomeScreen", "✅ بارگذاری تبلیغات انجام شد.")
    }

    // ✅ نمایش وضعیت بارگذاری در UI
    val statusText = buildString {
        appendLine("📊 وضعیت تبلیغات:")
        appendLine("adData: ${if (adData == null) "❌ null" else "✅ موجود"}")
        if (adData != null) {
            appendLine("  imageUrl: ${if (adData.imageUrl.isNullOrEmpty()) "❌ خالی" else "✅ ${adData.imageUrl?.take(50)}..."}")
            appendLine("  exists: ${adData.exists}")
        }
        appendLine("eitaaPost: ${if (eitaaPost == null) "❌ null" else "✅ موجود"}")
        if (eitaaPost != null) {
            appendLine("  text: ${if (eitaaPost.text.isNullOrBlank()) "❌ خالی" else "✅ ${eitaaPost.text.take(30)}..."}")
        }
    }

    // تابع باز کردن لینک
    fun openLink(link: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("HomeScreen", "❌ خطا در باز کردن لینک: ${e.message}")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "📚 المدونة",
                        fontSize = 22.sp * fontScale,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = { 
                        Log.d("HomeScreen", "🔄 رفرش دستی مطالب")
                        viewModel.fetchAllPosts() 
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "تحديث")
                    }
                    IconButton(onClick = { navController.navigate("settings") }) {
                        Icon(Icons.Default.Settings, contentDescription = "الإعدادات")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("جاري التحميل...")
                        }
                    }
                }
                errorMessage != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "❌ $errorMessage",
                                fontSize = 16.sp * fontScale,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { viewModel.fetchAllPosts() }) {
                                Text("🔄 إعادة المحاولة")
                            }
                        }
                    }
                }
                posts.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            // ✅ نمایش وضعیت دیباگ در صفحه
                            Text(
                                text = statusText,
                                fontSize = 12.sp * fontScale,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier.padding(8.dp)
                            )
                            Text(
                                "لا توجد مشاركات",
                                fontSize = 16.sp * fontScale,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // ✅ نمایش وضعیت دیباگ در بالای لیست
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 4.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Text(
                                    text = statusText,
                                    fontSize = 11.sp * fontScale,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        // ✅ کارت تبلیغاتی Worker (با کپی محلی ad)
                        if (ad != null && !ad.imageUrl.isNullOrEmpty()) {
                            Log.d("HomeScreen", "✅ نمایش کارت تبلیغاتی Worker")
                            item {
                                AdBanner(
                                    adData = ad,
                                    onAdClick = { link -> openLink(link) }
                                )
                            }
                        } else {
                            Log.d("HomeScreen", "❌ کارت تبلیغاتی Worker نمایش داده نشد (ad=$ad, imageUrl=${ad?.imageUrl})")
                        }

                        // ✅ کارت ایتا (با کپی محلی eitaa)
                        if (eitaa != null && !eitaa.text.isNullOrBlank()) {
                            Log.d("HomeScreen", "✅ نمایش کارت ایتا")
                            item {
                                EitaaBanner(
                                    eitaaPost = eitaa,
                                    onLinkClick = { link -> openLink(link) }
                                )
                            }
                        } else {
                            Log.d("HomeScreen", "❌ کارت ایتا نمایش داده نشد (eitaa=$eitaa, text=${eitaa?.text})")
                        }

                        // ✅ لیست مطالب
                        items(posts) { post ->
                            PostCard(
                                post = post,
                                onCardClick = { navController.navigate("post/${post.id}") }
                            )
                        }
                    }
                }
            }
        }
    }
}
