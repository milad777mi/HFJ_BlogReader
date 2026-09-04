package com.hfj.blogreader.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.hfj.blogreader.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    navController: NavController
) {
    // دریافت وضعیت‌ها
    val posts by viewModel.filteredPosts.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    // نمایش ساده
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📚 قارئ المدونة") }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            when {
                isLoading -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("جاري التحميل...")
                    }
                }
                errorMessage != null -> {
                    Text("❌ $errorMessage", color = MaterialTheme.colorScheme.error)
                }
                posts.isEmpty() -> {
                    Text("📭 لا توجد مشاركات")
                }
                else -> {
                    Text(
                        text = "✅ ${posts.size} مشاركة",
                        fontSize = 20.sp
                    )
                }
            }
        }
    }
}
