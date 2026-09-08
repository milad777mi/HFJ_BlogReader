package com.hfj.blogreader.data.models

data class AdData(
    val exists: Boolean = false,
    val imageUrl: String? = null,
    val link: String? = null,
    val title: String? = null,
    val updatedAt: Long = 0L
)
