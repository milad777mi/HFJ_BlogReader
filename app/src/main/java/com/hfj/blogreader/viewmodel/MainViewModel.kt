package com.hfj.blogreader.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hfj.blogreader.data.models.Post
import com.hfj.blogreader.data.repository.BlogRepository
import com.hfj.blogreader.utils.FontSizeManager
import com.hfj.blogreader.utils.BlogStats
import com.hfj.blogreader.utils.StatFetcher
import com.hfj.blogreader.utils.LikeManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val blogRepo = BlogRepository()
    private val fontManager = FontSizeManager(getApplication())

    val fontScale: StateFlow<Float> = fontManager.fontScale
    fun setFontScale(scale: Float) {
        fontManager.setFontScale(scale)
    }

    private val _allPosts = MutableStateFlow<List<Post>>(emptyList())
    val allPosts: StateFlow<List<Post>> = _allPosts

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    val filteredPosts: StateFlow<List<Post>> = _allPosts

    private val _stats = MutableStateFlow(BlogStats())
    val stats: StateFlow<BlogStats> = _stats

    // ✅ Likes
    private val _likes = MutableStateFlow<Map<String, Int>>(emptyMap())
    val likes: StateFlow<Map<String, Int>> = _likes

    private val _likedStatus = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val likedStatus: StateFlow<Map<String, Boolean>> = _likedStatus

    fun fetchAllPosts() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val posts = blogRepo.fetchAllPosts()
                _allPosts.value = posts
                if (posts.isEmpty()) {
                    _errorMessage.value = "⚠️ هیچ پستی یافت نشد"
                }
            } catch (e: Exception) {
                _errorMessage.value = "❌ خطا: ${e.message}"
                _allPosts.value = emptyList()
                e.printStackTrace()
            }
            _isLoading.value = false
        }
    }

    fun incrementStats(context: Context) {
        viewModelScope.launch {
            try {
                val result = StatFetcher.incrementAndFetchStats(context)
                _stats.value = result
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadStats(context: Context) {
        viewModelScope.launch {
            try {
                val result = StatFetcher.fetchStatsOnly()
                _stats.value = result
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // ✅ Like functions
    fun getLikeCount(postId: String): Int = _likes.value[postId] ?: 0
    fun isLiked(postId: String): Boolean = _likedStatus.value[postId] ?: false

    fun toggleLike(postId: String, userId: String) {
        viewModelScope.launch {
            try {
                val newCount = LikeManager.likePost(postId, userId)
                if (newCount > 0) {
                    _likes.update { current -> current + (postId to newCount) }
                    _likedStatus.update { current -> current + (postId to true) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadLikeStatus(postId: String, userId: String) {
        viewModelScope.launch {
            try {
                val liked = LikeManager.getLikeStatus(postId, userId)
                val count = LikeManager.getLikeCount(postId)
                _likedStatus.update { current -> current + (postId to liked) }
                _likes.update { current -> current + (postId to count) }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    init {
        fetchAllPosts()
    }
}
