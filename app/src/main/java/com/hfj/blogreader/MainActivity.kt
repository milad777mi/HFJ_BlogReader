package com.hfj.blogreader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.hfj.blogreader.ui.screens.HomeScreen
import com.hfj.blogreader.ui.screens.PostDetailScreen
import com.hfj.blogreader.ui.screens.SettingsScreen
import com.hfj.blogreader.ui.theme.HFJBlogReaderTheme
import com.hfj.blogreader.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel: MainViewModel = viewModel()
            val errorMessage by viewModel.errorMessage.collectAsState()
            val navController = rememberNavController()

            HFJBlogReaderTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (errorMessage != null) {
                        // نمایش پیام خطا
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("❌ $errorMessage")
                        }
                    } else {
                        NavHost(
                            navController = navController,
                            startDestination = "home"
                        ) {
                            composable("home") {
                                HomeScreen(
                                    viewModel = viewModel,
                                    navController = navController
                                )
                            }
                            composable("post/{postId}") { backStackEntry ->
                                val id = backStackEntry.arguments?.getString("postId") ?: ""
                                PostDetailScreen(
                                    postId = id,
                                    viewModel = viewModel,
                                    navController = navController
                                )
                            }
                            composable("settings") {
                                SettingsScreen(
                                    viewModel = viewModel,
                                    context = this@MainActivity
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
