package com.hfj.blogreader.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.hfj.blogreader.ui.components.PostCard
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

    // دریافت داده‌های تبلیغاتی
    val adData by viewModel.adData.collectAsState()
    val eitaaPost by viewModel.eitaaPost.collectAsState()

    // کپی محلی برای Smart cast
    val ad = adData
    val eitaa = eitaaPost

    // وضعیت نمایش پاپ‌آپ‌ها
    var showAdDialog by remember { mutableStateOf(false) }
    var showEitaaDialog by remember { mutableStateOf(false) }

    // بارگذاری خودکار هنگام ورود
    LaunchedEffect(Unit) {
        viewModel.loadAdData()
        viewModel.loadEitaaPost()
    }

    // اگر تبلیغات وجود داشته باشد، پاپ‌آپ را نشان بده
    LaunchedEffect(ad) {
        if (ad != null && !ad.imageUrl.isNullOrEmpty()) {
            showAdDialog = true
        }
    }

    // اگر ایتا وجود داشته باشد، پاپ‌آپ را نشان بده
    LaunchedEffect(eitaa) {
        if (eitaa != null && !eitaa.text.isNullOrBlank()) {
            showEitaaDialog = true
        }
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
                        // لیست مطالب
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

    // ========== پاپ‌آپ تبلیغات Worker ==========
    if (showAdDialog && ad != null) {
        Dialog(
            onDismissRequest = { showAdDialog = false }
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // تصویر تبلیغات
                    AsyncImage(
                        model = ad.imageUrl,
                        contentDescription = ad.title ?: "تبلیغات",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // عنوان
                    if (!ad.title.isNullOrEmpty()) {
                        Text(
                            text = ad.title,
                            fontSize = 18.sp * fontScale,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // دکمه‌ها
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // دکمه مشاهده (اگر لینک وجود داشته باشد)
                        if (!ad.link.isNullOrEmpty()) {
                            Button(
                                onClick = {
                                    openLink(ad.link)
                                    showAdDialog = false
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("مشاهدة")
                            }
                        }

                        // دکمه بستن
                        Button(
                            onClick = { showAdDialog = false },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Text("إغلاق")
                        }
                    }
                }
            }
        }
    }

    // ========== پاپ‌آپ ایتا ==========
    if (showEitaaDialog && eitaa != null) {
        Dialog(
            onDismissRequest = { showEitaaDialog = false }
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = eitaa.text,
                        fontSize = 16.sp * fontScale,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // اگر لینک وجود داشته باشد
                        if (!eitaa.link.isNullOrEmpty()) {
                            Button(
                                onClick = {
                                    openLink(eitaa.link)
                                    showEitaaDialog = false
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("مشاهدة")
                            }
                        }

                        // دکمه بستن
                        Button(
                            onClick = { showEitaaDialog = false },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Text("إغلاق")
                        }
                    }
                }
            }
        }
    }
}
