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
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.compose.LocalImageLoader
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

    val adData by viewModel.adData.collectAsState()
    val eitaaPost by viewModel.eitaaPost.collectAsState()
    val adTextItems by viewModel.adTextItems.collectAsState()

    val ad = adData
    val eitaa = eitaaPost

    fun openLink(link: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
            context.startActivity(intent)
        } catch (e: Exception) { }
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
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // ============================================
                        // ✅ کارت‌های تبلیغات متنی (۲ پیام از کانال تلگرام)
                        // ============================================
                        items(
                            items = adTextItems,
                            key = { "adtext_${it.id}" }
                        ) { adText ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp)
                                ) {
                                    Text(
                                        text = adText.text,
                                        fontSize = 14.sp * fontScale,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    if (!adText.link.isNullOrEmpty()) {
                                        TextButton(
                                            onClick = { openLink(adText.link) },
                                            modifier = Modifier.padding(top = 4.dp)
                                        ) {
                                            Text("🔗 مشاهده", fontSize = 12.sp * fontScale)
                                        }
                                    }
                                }
                            }
                        }

                        // ============================================
                        // کارت تبلیغاتی تصویری
                        // ============================================
                        if (ad != null && !ad.imageUrl.isNullOrEmpty()) {
                            item(key = "ad_banner") {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    shape = RoundedCornerShape(18.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    ),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        AsyncImage(
                                            model = ad.imageUrl,
                                            contentDescription = ad.title ?: "تبلیغات",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(150.dp)
                                                .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)),
                                            contentScale = ContentScale.FillBounds,
                                            imageLoader = LocalImageLoader.current
                                        )
                                        if (!ad.title.isNullOrEmpty()) {
                                            Text(
                                                text = ad.title,
                                                fontSize = 13.sp * fontScale,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                            )
                                        }
                                        if (!ad.link.isNullOrEmpty()) {
                                            TextButton(
                                                onClick = { openLink(ad.link) },
                                                modifier = Modifier.padding(horizontal = 14.dp)
                                                    .padding(bottom = 8.dp)
                                            ) {
                                                Text("🔗 مشاهده", fontSize = 12.sp * fontScale)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // ============================================
                        // کارت ایتا
                        // ============================================
                        if (eitaa != null && !eitaa.text.isNullOrBlank()) {
                            item(key = "eitaa_banner") {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    shape = RoundedCornerShape(18.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer
                                    ),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp)
                                    ) {
                                        Text(
                                            text = eitaa.text,
                                            fontSize = 14.sp * fontScale,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        if (!eitaa.link.isNullOrEmpty()) {
                                            TextButton(
                                                onClick = { openLink(eitaa.link) },
                                                modifier = Modifier.padding(top = 4.dp)
                                            ) {
                                                Text("🔗 مشاهده", fontSize = 12.sp * fontScale)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // ============================================
                        // لیست مطالب
                        // ============================================
                        items(
                            items = posts,
                            key = { post -> "post_${post.id}" }
                        ) { post ->
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
