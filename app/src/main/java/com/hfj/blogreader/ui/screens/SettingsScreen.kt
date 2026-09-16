package com.hfj.blogreader.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hfj.blogreader.ui.theme.LocalFontScale
import com.hfj.blogreader.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    context: Context
) {
    val fontScale by viewModel.fontScale.collectAsState()
    val stats by viewModel.stats.collectAsState()

    // ✅ تابع باز کردن لینک
    fun openTelegramLink() {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/MMSNETBNM"))
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "⚙️ الإعدادات",
                        fontSize = 21.sp * fontScale,
                        fontWeight = FontWeight.Bold
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
            // ========== کارت آمار ==========
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "📊 إحصائيات المدونة",
                        fontSize = 16.sp * fontScale,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    StatRow("اليوم", stats.today, fontScale)
                    StatRow("الإجمالي", stats.total, fontScale)
                }
            }

            // ========== کارت حجم الخط ==========
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
                        fontWeight = FontWeight.Medium
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
                            fontWeight = FontWeight.Bold
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
                            Icon(Icons.Default.Remove, contentDescription = "تصغير")
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
                            Icon(Icons.Default.Add, contentDescription = "تكبير")
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

            // ========== کارت حول (با لینک روی MMSNETBNM) ==========
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
                        "📱 حول",
                        fontSize = 16.sp * fontScale,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    // ✅ ساخت متن با annotation
                    val annotatedString = buildAnnotatedString {
                        // 1️⃣ خط اول
                        withStyle(SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )) {
                            append("MMS.NET BNM.J")
                        }
                        append("\n")
                        append("Build • Network • Media\n")
                        append("علامة رقمية في مجال التقنية، البرمجة، الإنترنت، الجرافيك والإعلام الرقمي.\n")
                        append("نمزج بين الفن والتقنية لبناء برامج وحلول رقمية ومحتوى حديث.\n\n")
                        append("© 2023   2026\n")
                        append("جميع الحقوق محفوظة لـ\n")

                        // 2️⃣ بعد از «جميع الحقوق»
                        withStyle(SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )) {
                            append("MMS.NET BNM.J")
                        }
                        append("\n")
                        append("يُمنع نقل أو تقليد برامجنا وقوالبنا وأعمالنا، أو إعادة استخدامها دون إذن. كما يُمنع استخدام أفكارنا وطرق عملنا أو إعادة توظيفها دون إذن مسبق.\n")
                        append("وتحتفظ ")

                        // 3️⃣ داخل جمله
                        withStyle(SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )) {
                            append("MMS.NET BNM.J")
                        }
                        append(" بحقها في اتخاذ كافة الإجراءات القانونية بحق كل من يخالف هذه الحقوق.\n\n")
                        append("made by: ")

                        // 4️⃣ در «made by»
                        withStyle(SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )) {
                            append("MMS.NET BNM.J")
                        }
                        append("\ntelegram ch: ")

                        // ✅ 5️⃣ لینک کلیک‌پذیر روی MMSNETBNM
                        pushStringAnnotation(
                            tag = "TELEGRAM_LINK",
                            annotation = "https://t.me/MMSNETBNM"
                        )
                        withStyle(SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            textDecoration = TextDecoration.Underline
                        )) {
                            append("MMSNETBNM")
                        }
                        pop()

                        append("\ntelegram: MMSNETBNMBOT\n\n")
                        append("مدونة حسين فاضل الجنامي\n")
                        append("1.0.0")
                    }

                    // ✅ استفاده از ClickableText
                    ClickableText(
                        text = annotatedString,
                        style = TextStyle(
                            fontSize = 13.sp * fontScale,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            lineHeight = 20.sp
                        ),
                        onClick = { offset ->
                            annotatedString
                                .getStringAnnotations(
                                    tag = "TELEGRAM_LINK",
                                    start = offset,
                                    end = offset
                                )
                                .firstOrNull()
                                ?.let {
                                    openTelegramLink()
                                }
                        }
                    )
                }
            }

            // ========== دکمه بارگذاری مجدد مطالب ==========
            Button(
                onClick = { viewModel.fetchAllPosts() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("🔄 إعادة تحميل المشاركات")
            }
        }
    }
}

@Composable
fun StatRow(label: String, value: String, fontScale: Float) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 14.sp * fontScale)
        Text(
            text = value,
            fontSize = 14.sp * fontScale,
            fontWeight = FontWeight.Bold
        )
    }
}
