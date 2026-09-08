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

    // ✅ دریافت داده‌های کارت‌های تبلیغاتی
    val adData by viewModel.adData.collectAsState()
    val eitaaPost by viewModel.eitaaPost.collectAsState()

    // ✅ کپی محلی برای Smart cast
    val ad = adData
    val eitaa = eitaaPost

    // ✅ بارگذاری کارت‌ها هنگام ورود به صفحه
    LaunchedEffect(Unit) {
        viewModel.loadAdData()
        viewModel.loadEitaaPost()
    }

    // تابع باز کردن لینک در مرورگر
    fun openLink(link: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
            context.startActivity(intent)
        } catch (e: Exception) {
            // اگر لینک نامعتبر بود، کاری نکن
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
                        Text(
                            "لا توجد مشاركات",
                            fontSize = 16.sp * fontScale,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // ✅ کارت تبلیغاتی Worker (با کپی محلی ad)
                        if (ad != null && ad.exists && !ad.imageUrl.isNullOrEmpty()) {
                            item {
                                AdBanner(
                                    adData = ad,
                                    onAdClick = { link -> openLink(link) }
                                )
                            }
                        }

                        // ✅ کارت ایتا (با کپی محلی eitaa)
                        if (eitaa != null && !eitaa.text.isNullOrBlank()) {
                            item {
                                EitaaBanner(
                                    eitaaPost = eitaa,
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
