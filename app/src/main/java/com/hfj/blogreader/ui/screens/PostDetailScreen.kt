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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.hfj.blogreader.ui.components.ZoomableImage
import com.hfj.blogreader.ui.theme.LocalFontScale
import com.hfj.blogreader.viewmodel.MainViewModel
import com.hfj.blogreader.utils.UserManager
import java.text.SimpleDateFormat
import java.util.*

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
    var zoomImageUrl by remember { mutableStateOf<String?>(null) }

    val userId = UserManager.getUserId(context)
    val likedStatus by viewModel.likedStatus.collectAsState()
    val likes by viewModel.likes.collectAsState()

    val isLiked = likedStatus[postId] ?: false
    val likeCount = likes[postId] ?: 0

    // نظرات
    val commentList = viewModel.comments.value[postId] ?: emptyList()

    // حالت‌های فرم نظر
    var commentText by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var submitMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(postId) {
        viewModel.loadLikeStatus(postId, userId)
        viewModel.loadComments(postId)
    }

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
            // ========== فیلم ==========
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

            // ========== تصاویر ==========
            if (post.videoUrl == null && post.imageUrls.isNotEmpty()) {
                post.imageUrls.forEach { url ->
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                            .padding(bottom = 12.dp)
                            .clickable { zoomImageUrl = url },
                        contentScale = ContentScale.Crop
                    )
                }
            }

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
                            fontWeight = FontWeight.Bold,
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row {
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

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable {
                                    if (!isLiked) {
                                        viewModel.toggleLike(postId, userId)
                                    }
                                }
                                .padding(4.dp)
                        ) {
                            Icon(
                                if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "لایک",
                                tint = if (isLiked) Color.Red else Color.Gray,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = likeCount.toString(),
                                fontSize = 16.sp,
                                color = if (isLiked) Color.Red else Color.Gray,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // ========== 💬 بخش نظرات ==========
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                // عنوان نظرات
                Text(
                    text = "💬 التعليقات (${commentList.size})",
                    fontSize = 17.sp * fontScale,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))

                // لیست نظرات
                if (commentList.isEmpty()) {
                    Text(
                        text = "لا توجد تعليقات حتى الآن",
                        fontSize = 14.sp * fontScale,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                } else {
                    commentList.forEach { comment ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = comment.userName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp * fontScale
                                    )
                                    Text(
                                        text = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US)
                                            .format(Date(comment.timestamp)),
                                        fontSize = 11.sp * fontScale,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = comment.text,
                                    fontSize = 15.sp * fontScale,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ========== فرم ثبت نظر ==========
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        // پیام وضعیت
                        if (submitMessage != null) {
                            Text(
                                text = submitMessage!!,
                                fontSize = 13.sp * fontScale,
                                color = if (submitMessage!!.contains("✅")) Color.Green else Color.Red,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }

                        // فیلد ورودی نظر
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = commentText,
                                onValueChange = { 
                                    if (it.length <= 66) commentText = it 
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp),
                                placeholder = { 
                                    Text(
                                        "اكتب اسمك و تعليقك (حد 66 حرف)",
                                        fontSize = 13.sp * fontScale
                                    ) 
                                },
                                maxLines = 2,
                                enabled = !isSubmitting,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                )
                            )

                            IconButton(
                                onClick = {
                                    if (commentText.isNotBlank() && !isSubmitting) {
                                        isSubmitting = true
                                        submitMessage = null
                                        viewModel.submitComment(
                                            postId,
                                            userId,
                                            "مستخدم",
                                            commentText.trim()
                                        )
                                        submitMessage = "✅ تم إرسال تعليقك للمراجعة"
                                        commentText = ""
                                        isSubmitting = false
                                    }
                                },
                                enabled = commentText.isNotBlank() && !isSubmitting
                            ) {
                                Icon(
                                    Icons.Default.Send,
                                    contentDescription = "إرسال",
                                    tint = if (commentText.isNotBlank() && !isSubmitting)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                )
                            }
                        }

                        // شمارنده کاراکترها
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = "${commentText.length}/66",
                                fontSize = 11.sp * fontScale,
                                color = if (commentText.length > 66) Color.Red else Color.Gray
                            )
                        }

                        // ✅ متن راهنما (جایگزین دکمه تحديث)
                        Text(
                            text = """
                                في كل يوم يمكنك تقديم تعليق واحد لكل مشاركة.
                                الحد الأقصى 5 تعليقات يومياً للمستخدم الواحد.
                                التعليقات تظهر بعد الموافقة عليها.
                                التعليقات التي تتجاوز الحد المسموح لا يتم تسجيلها أو عرضها.
                            """.trimIndent(),
                            fontSize = 11.sp * fontScale,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }
    }

    // ========== دیالوگ بزرگنمایی فیلم ==========
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

    // ========== دیالوگ بزرگنمایی تصویر ==========
    if (zoomImageUrl != null) {
        Dialog(
            onDismissRequest = { zoomImageUrl = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.9f))
                    .clickable { zoomImageUrl = null }
            ) {
                ZoomableImage(
                    model = zoomImageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}
