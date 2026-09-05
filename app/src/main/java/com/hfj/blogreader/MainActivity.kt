package com.hfj.blogreader

import android.os.Bundle
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.hfj.blogreader.ui.screens.HomeScreen
import com.hfj.blogreader.ui.screens.PostDetailScreen
import com.hfj.blogreader.ui.screens.SettingsScreen
import com.hfj.blogreader.ui.theme.HFJBlogReaderTheme
import com.hfj.blogreader.ui.theme.LocalFontScale
import com.hfj.blogreader.utils.CrashHandler
import com.hfj.blogreader.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private lateinit var crashHandler: CrashHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        crashHandler = CrashHandler(this)

        // بررسی وجود لاگ کرش
        val crashLog = crashHandler.getCrashLog()
        if (crashLog != null) {
            showCrashLogDialog(crashLog)
            crashHandler.clearCrashLog()
            return
        }

        // اجرای عادی برنامه
        setContent {
            val viewModel: MainViewModel = viewModel()
            val fontScale by viewModel.fontScale.collectAsState()
            val navController = rememberNavController()
            val context = LocalContext.current

            // ✅ افزایش آمار فقط یک بار هنگام باز شدن برنامه
            LaunchedEffect(Unit) {
                viewModel.incrementStats(context)
            }

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
    }

    private fun showCrashLogDialog(log: String) {
        val textView = TextView(this).apply {
            text = log
            setTextIsSelectable(true)
            textSize = 12f
            setPadding(32, 32, 32, 32)
        }

        AlertDialog.Builder(this)
            .setTitle("📄 گزارش خطا (کرش)")
            .setView(textView)
            .setPositiveButton("📤 اشتراک‌گذاری") { _, _ ->
                val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(android.content.Intent.EXTRA_TEXT, log)
                }
                startActivity(android.content.Intent.createChooser(intent, "ارسال لاگ"))
            }
            .setNegativeButton("❌ بستن") { _, _ ->
                finish()
            }
            .setNeutralButton("🔄 اجرای مجدد") { _, _ ->
                val intent = packageManager.getLaunchIntentForPackage(packageName)
                intent?.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(intent)
                finish()
            }
            .setCancelable(false)
            .show()
    }
}
