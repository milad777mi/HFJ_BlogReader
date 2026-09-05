package com.hfj.blogreader.data.models

data class Comment(
    val id: String,
    val postId: String,
    val userId: String,
    val userName: String,
    val text: String,
    val timestamp: Long,
    val approvedAt: Long? = null
)
