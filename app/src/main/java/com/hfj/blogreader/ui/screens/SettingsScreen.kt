package com.hfj.blogreader.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add          // ← اضافه شد
import androidx.compose.material.icons.filled.Remove       // ← اضافه شد
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hfj.blogreader.ui.theme.LocalFontScale
import com.hfj.blogreader.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    context: Context
) {
    val fontScale by viewModel.fontScale.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "⚙️ الإعدادات",
                        fontSize = 21.sp * fontScale,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // کارت حجم الخط
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        "🔤 حجم الخط",
                        fontSize = 16.sp * fontScale,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("صغير", fontSize = 12.sp * fontScale)
                        Text(
                            "${(fontScale * 100).toInt()}%",
                            fontSize = 14.sp * fontScale,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                        Text("كبير", fontSize = 12.sp * fontScale)
                    }

                    Slider(
                        value = fontScale,
                        onValueChange = { viewModel.setFontScale(it) },
                        valueRange = 0.7f..1.8f,
                        steps = 10,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        IconButton(
                            onClick = {
                                val newValue = (fontScale - 0.05f).coerceAtLeast(0.7f)
                                viewModel.setFontScale(newValue)
                            }
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "تصغير")   // ← اصلاح شد
                        }

                        Text(
                            "${(fontScale * 100).toInt()}%",
                            fontSize = 18.sp * fontScale,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        IconButton(
                            onClick = {
                                val newValue = (fontScale + 0.05f).coerceAtMost(1.8f)
                                viewModel.setFontScale(newValue)
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "تكبير")      // ← اصلاح شد
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        "نص تجريبي بالحجم الحالي",
                        fontSize = 16.sp * fontScale,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )

                    Button(
                        onClick = { viewModel.setFontScale(1.0f) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Text("↩️ إعادة تعيين")
                    }
                }
            }

            // کارت حول التطبيق
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        "📱 حول التطبيق",
                        fontSize = 16.sp * fontScale,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "قارئ مدونة حسين فاضل الجنامي\nالإصدار 1.0.0",
                        fontSize = 13.sp * fontScale,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }

            // دکمه بارگذاری مجدد
            Button(
                onClick = { viewModel.fetchAllPosts() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("🔄 إعادة تحميل المشاركات")
            }
        }
    }
}
