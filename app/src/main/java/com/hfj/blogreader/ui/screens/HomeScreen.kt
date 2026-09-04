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
    val posts by viewModel.filteredPosts.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📚 قارئ المدونة") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("جاري التحميل...")
                }
                errorMessage != null -> {
                    Text("❌ $errorMessage", color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { viewModel.fetchAllPosts() }) {
                        Text("🔄 إعادة المحاولة")
                    }
                }
                posts.isEmpty() -> {
                    Text("📭 لا توجد مشاركات")
                }
                else -> {
                    Text("✅ ${posts.size} مشاركة تم تحميلها!")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "مثال: ${posts.firstOrNull()?.content?.take(50) ?: "..."}",
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
