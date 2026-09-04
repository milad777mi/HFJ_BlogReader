package com.hfj.blogreader.data.models

data class Post(
    val id: String,
    val title: String? = null,
    val content: String,
    val imageUrl: String? = null,
    val videoUrl: String? = null,
    val date: String,
    val hashtags: List<String> = emptyList(),
    val views: String = "0"
)
