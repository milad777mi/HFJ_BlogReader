package com.hfj.blogreader.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hfj.blogreader.data.models.Post
import com.hfj.blogreader.data.models.Comment
import com.hfj.blogreader.data.repository.BlogRepository
import com.hfj.blogreader.utils.FontSizeManager
import com.hfj.blogreader.utils.BlogStats
import com.hfj.blogreader.utils.StatFetcher
import com.hfj.blogreader.utils.LikeManager
import com.hfj.blogreader.utils.CommentManager
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

    // Likes
    private val _likes = MutableStateFlow<Map<String, Int>>(emptyMap())
    val likes: StateFlow<Map<String, Int>> = _likes

    private val _likedStatus = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val likedStatus: StateFlow<Map<String, Boolean>> = _likedStatus

    // ✅ Comments
    private val _comments = MutableStateFlow<Map<String, List<Comment>>>(emptyMap())
    val comments: StateFlow<Map<String, List<Comment>>> = _comments

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

    // Like functions
    fun getLikeCount(postId: String): Int = _likes.value[postId] ?: 0
    fun isLiked(postId: String): Boolean = _likedStatus.value[postId] ?: false

    fun toggleLike(postId: String, userId: String) {
        viewModelScope.launch {
            try {
                val newCount = LikeManager.likePost(postId, userId)
                if (newCount > 0) {
                    val currentLikes = _likes.value.toMutableMap()
                    currentLikes[postId] = newCount
                    _likes.value = currentLikes

                    val currentStatus = _likedStatus.value.toMutableMap()
                    currentStatus[postId] = true
                    _likedStatus.value = currentStatus
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

                val currentStatus = _likedStatus.value.toMutableMap()
                currentStatus[postId] = liked
                _likedStatus.value = currentStatus

                val currentLikes = _likes.value.toMutableMap()
                currentLikes[postId] = count
                _likes.value = currentLikes
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // ✅ Comment functions
    fun loadComments(postId: String) {
        viewModelScope.launch {
            try {
                val list = CommentManager.getApprovedComments(postId)
                val currentMap = _comments.value.toMutableMap()
                currentMap[postId] = list
                _comments.value = currentMap
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun submitComment(postId: String, userId: String, userName: String, text: String) {
        viewModelScope.launch {
            try {
                val success = CommentManager.submitComment(postId, userId, userName, text)
                if (success) {
                    // نظر برای تایید ارسال شد، می‌توانیم یک پیام موفقیت نمایش دهیم
                    // اما نظرات بلافاصله نمایش داده نمی‌شوند تا تایید شوند
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    init {
        fetchAllPosts()
    }
}
