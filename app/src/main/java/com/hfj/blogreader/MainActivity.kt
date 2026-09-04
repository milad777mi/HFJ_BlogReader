package com.hfj.blogreader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
                try {
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
                } catch (e: Exception) {
                    // نمایش خطا در خود صفحه (بدون Toast)
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text("❌ خطا در بارگذاری برنامه")
                            Text("${e.javaClass.simpleName}: ${e.message}")
                            e.stackTrace.forEach {
                                Text("${it.toString().take(100)}")
                            }
                        }
                    }
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            // اگر خود setContent هم کرش کرد، نمی‌توانیم UI نشان دهیم
            // اینجا فقط لاگ می‌کنیم (در گوشی دیده نمی‌شود)
            e.printStackTrace()
            // برای اینکه حداقل چیزی نشان دهد، یک View ساده می‌سازیم (اختیاری)
            setContentView(android.widget.TextView(this).apply {
                text = "خطا در onCreate: ${e.message}"
                setTextColor(android.graphics.Color.RED)
                textSize = 20f
                setPadding(50, 50, 50, 50)
            })
        }
    }
}
