package com.hfj.blogreader.data.models

data class Post(
    val id: String,
    val title: String? = null,
    val content: String,
    val imageUrls: List<String> = emptyList(),
    val videoUrl: String? = null,
    val date: String,
    val hashtags: List<String> = emptyList(),  // ← خالی (هشتگ نداریم)
    val views: String = "0"
)
