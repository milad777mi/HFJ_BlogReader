package com.hfj.blogreader.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.hfj.blogreader.data.models.Post
import com.hfj.blogreader.ui.theme.LocalFontScale

@Composable
fun PostCard(
    post: Post,
    onCardClick: () -> Unit,
    onImageClick: (String) -> Unit = {}
) {
    val fontScale = LocalFontScale.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() },
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // ✅ نمایش تصویر/فیلم در کارت
            when {
                // 1. هم عکس و هم فیلم دارد → عکس به‌عنوان پیش‌نمایش فیلم
                post.imageUrls.isNotEmpty() && post.videoUrl != null -> {
                    val imageUrl = post.imageUrls.first()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp)
                            .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                    ) {
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable { onImageClick(imageUrl) },
                            contentScale = ContentScale.Crop
                        )
                        // آیکون پلی روی عکس
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = "فيلم",
                                modifier = Modifier.size(56.dp),
                                tint = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
                // 2. فقط فیلم دارد → آیکون پلی
                post.videoUrl != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp)
                            .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { onCardClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = "فيديو",
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "فيلم",
                                fontSize = 14.sp * fontScale,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                // 3. فقط عکس دارد → عکس کامل
                post.imageUrls.isNotEmpty() -> {
                    if (post.imageUrls.size == 1) {
                        AsyncImage(
                            model = post.imageUrls.first(),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(190.dp)
                                .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                                .clickable { onImageClick(post.imageUrls.first()) },
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(190.dp)
                                .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                        ) {
                            items(post.imageUrls) { url ->
                                AsyncImage(
                                    model = url,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .width(200.dp)
                                        .fillMaxHeight()
                                        .clickable { onImageClick(url) },
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }
                // 4. هیچ عکس و فیلمی ندارد → هیچ چیز نمایش داده نشود
                else -> { /* خالی */ }
            }

            // ========== محتوای کارت ==========
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                // عنوان جدا با فونت بزرگتر
                if (post.title != null && post.title.isNotEmpty()) {
                    Text(
                        text = post.title,
                        fontSize = 17.sp * fontScale,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 4.dp),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // متن کوتاه (۳ خط)
                Text(
                    text = post.content,
                    fontSize = 14.sp * fontScale,
                    lineHeight = 24.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                // ========== تاریخ (بدون عبارت اضافی) ==========
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        post.date,  // ← فقط تاریخ، بدون "نوشته شده در"
                        fontSize = 12.sp * fontScale,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}
