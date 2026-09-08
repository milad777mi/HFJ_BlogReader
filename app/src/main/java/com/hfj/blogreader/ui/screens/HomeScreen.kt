package com.hfj.blogreader.ui.screens

import android.content.Intent
import android.net.Uri
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

    // ✅ بارگذاری خودکار هنگام ورود
    LaunchedEffect(Unit) {
        viewModel.loadAdData()
        viewModel.loadEitaaPost()
    }

    // ✅ لاگ‌های دیباگ برای بررسی وضعیت adData
    LaunchedEffect(adData) {
        if (adData != null) {
            android.util.Log.d("HomeScreen", "✅ Ad Data: exists=${adData.exists}, imageUrl=${adData.imageUrl}, title=${adData.title}")
            println("✅ Ad Data: exists=${adData.exists}, imageUrl=${adData.imageUrl}")
        } else {
            android.util.Log.d("HomeScreen", "❌ Ad Data is null")
            println("❌ Ad Data is null")
        }
    }

    // ✅ نمایش وضعیت بارگذاری تبلیغات (برای دیباگ)
    val adStatus = if (adData == null) {
        "⏳ در حال بارگذاری تبلیغات..."
    } else if (!adData.exists) {
        "📭 تبلیغات موجود نیست"
    } else if (adData.imageUrl.isNullOrEmpty()) {
        "⚠️ آدرس تصویر تبلیغات خالی است"
    } else {
        "✅ تبلیغات بارگذاری شد"
    }

    // تابع باز کردن لینک
    fun openLink(link: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
            context.startActivity(intent)
        } catch (e: Exception) {
            // خطا را نادیده بگیر
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
                    IconButton(onClick = { viewModel.fetchAllPosts() }) {
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
                            // ✅ نمایش وضعیت تبلیغات در صفحه (برای دیباگ)
                            Text(
                                text = adStatus,
                                fontSize = 14.sp * fontScale,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                modifier = Modifier.padding(bottom = 8.dp)
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
                        // ✅ نمایش وضعیت تبلیغات (فقط برای دیباگ)
                        item {
                            Text(
                                text = adStatus,
                                fontSize = 12.sp * fontScale,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                            )
                        }

                        // ✅ کارت تبلیغاتی Worker (در صورت وجود)
                        if (adData != null && adData.exists && !adData.imageUrl.isNullOrEmpty()) {
                            item {
                                AdBanner(
                                    adData = adData,
                                    onAdClick = { link -> openLink(link) }
                                )
                            }
                        }

                        // ✅ کارت ایتا (در صورت وجود)
                        if (eitaaPost != null && !eitaaPost.text.isNullOrBlank()) {
                            item {
                                EitaaBanner(
                                    eitaaPost = eitaaPost,
                                    onLinkClick = { link -> openLink(link) }
                                )
                            }
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
