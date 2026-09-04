package com.hfj.blogreader.data.models

data class Post(
    val id: String,
    val title: String? = null,
    val content: String,
    val imageUrls: List<String> = emptyList(),  // چند عکس
    val videoUrl: String? = null,
    val date: String,
    val hashtags: List<String> = emptyList(),   // هشتگ نداریم
    val views: String = "0"
)
