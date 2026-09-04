package com.hfj.blogreader.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.hfj.blogreader.ui.components.HashtagTabs
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
    val posts by viewModel.filteredPosts.collectAsStateWithLifecycle()
    val tabs by viewModel.hashtagTabs.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val fontScale = LocalFontScale.current

    // نمایش خطا در صورت وجود
    LaunchedEffect(Unit) {
        try {
            // اگر خطایی در ViewModel باشد، اینجا نمایش داده می‌شود
        } catch (e: Exception) {
            Toast.makeText(context, "❌ خطا: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "📚 قارئ المدونة",
                        fontSize = 22.sp * fontScale,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = {
                        try {
                            viewModel.fetchAllPosts()
                        } catch (e: Exception) {
                            Toast.makeText(context, "❌ خطا در بارگذاری: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
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
            if (tabs.isNotEmpty()) {
                HashtagTabs(
                    tabs = tabs,
                    selectedTab = selectedTab,
                    onTabSelected = { viewModel.selectTab(it) }
                )
            }

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (posts.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Text(
                        "لا توجد مشاركات",
                        fontSize = 16.sp * fontScale,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(posts) { post ->
                        PostCard(
                            post = post,
                            onHashtagTap = { tag -> viewModel.selectTab(tag) },
                            onCardClick = { navController.navigate("post/${post.id}") }
                        )
                    }
                }
            }
        }
    }
}
