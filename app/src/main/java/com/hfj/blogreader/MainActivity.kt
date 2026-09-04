package com.hfj.blogreader

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.hfj.blogreader.ui.screens.HomeScreen
import com.hfj.blogreader.ui.screens.PostDetailScreen
import com.hfj.blogreader.ui.screens.SettingsScreen
import com.hfj.blogreader.ui.theme.HFJBlogReaderTheme
import com.hfj.blogreader.ui.theme.LocalFontScale
import com.hfj.blogreader.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            setContent {
                val viewModel: MainViewModel = viewModel()
                val fontScale by viewModel.fontScale.collectAsState()
                val navController = rememberNavController()

                HFJBlogReaderTheme {
                    CompositionLocalProvider(
                        LocalFontScale provides fontScale
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background
                        ) {
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
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "❌ خطا در بارگذاری برنامه: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
            e.printStackTrace()
        }
    }
}
