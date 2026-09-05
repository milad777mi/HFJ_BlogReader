package com.hfj.blogreader.ui.screens

import android.net.Uri
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import com.hfj.blogreader.ui.theme.LocalFontScale
import com.hfj.blogreader.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    postId: String,
    viewModel: MainViewModel,
    navController: NavController
) {
    val context = LocalContext.current
    val posts by viewModel.filteredPosts.collectAsState()
    val post = posts.find { it.id == postId }
    val fontScale = LocalFontScale.current

    var showFullscreenVideo by remember { mutableStateOf(false) }

    if (post == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("المشاركة غير موجودة")
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "📖 تفاصيل المشاركة",
                        fontSize = 21.sp * fontScale,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "رجوع")
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            // ========== فقط فیلم (در صورت وجود) ==========
            if (post.videoUrl != null) {
                val exoPlayer = remember {
                    ExoPlayer.Builder(context).build().apply {
                        setMediaItem(MediaItem.fromUri(Uri.parse(post.videoUrl)))
                        prepare()
                        playWhenReady = false
                    }
                }

                DisposableEffect(Unit) {
                    onDispose {
                        exoPlayer.release()
                    }
                }

                // مکث پلیر هنگام باز شدن دیالوگ تمام‌صفحه
                LaunchedEffect(showFullscreenVideo) {
                    if (showFullscreenVideo) {
                        exoPlayer.pause()
                    }
                }

                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = true
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                            )
                            minimumHeight = 250.dp.value.toInt()
                            setOnClickListener {
                                showFullscreenVideo = true
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .padding(bottom = 12.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            }

            // ========== ❌ عکس حذف شد (فقط در کارت نمایش داده می‌شود) ==========

            // ========== محتوا ==========
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    if (post.title != null && post.title.isNotEmpty()) {
                        Text(
                            post.title!!,
                            fontSize = 22.sp * fontScale,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Text(
                        text = post.content,
                        fontSize = 17.sp * fontScale,
                        lineHeight = 34.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(12.dp))

                    // تاریخ (بدون "ساعت")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Icon(
                            Icons.Default.DateRange,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            post.date,
                            fontSize = 13.sp * fontScale,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }

    // ========== دیالوگ بزرگنمایی فیلم (تمام‌صفحه) ==========
    if (showFullscreenVideo) {
        val fullscreenPlayer = remember {
            ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri(Uri.parse(post.videoUrl)))
                prepare()
                playWhenReady = true
            }
        }

        DisposableEffect(Unit) {
            onDispose {
                fullscreenPlayer.release()
            }
        }

        Dialog(
            onDismissRequest = {
                showFullscreenVideo = false
                fullscreenPlayer.release()
            },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .clickable { showFullscreenVideo = false }
            ) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = fullscreenPlayer
                            useController = true
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
