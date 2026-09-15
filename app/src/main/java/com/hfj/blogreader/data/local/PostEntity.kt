package com.hfj.blogreader.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.hfj.blogreader.data.models.Post

@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey val id: String,
    val title: String?,
    val content: String,
    val imageUrls: String,       // به صورت JSON string ذخیره می‌شه
    val videoUrl: String?,
    val date: String,
    val hashtags: String,        // به صورت JSON string ذخیره می‌شه
    val views: String,
    val cachedAt: Long = System.currentTimeMillis()  // زمان کش شدن
) {
    fun toPost(): Post = Post(
        id = id,
        title = title,
        content = content,
        imageUrls = if (imageUrls.isBlank()) emptyList() else imageUrls.split("|"),
        videoUrl = videoUrl,
        date = date,
        hashtags = if (hashtags.isBlank()) emptyList() else hashtags.split("|"),
        views = views
    )

    companion object {
        fun fromPost(post: Post): PostEntity = PostEntity(
            id = post.id,
            title = post.title,
            content = post.content,
            imageUrls = post.imageUrls.joinToString("|"),
            videoUrl = post.videoUrl,
            date = post.date,
            hashtags = post.hashtags.joinToString("|"),
            views = post.views,
            cachedAt = System.currentTimeMillis()
        )
    }
}
