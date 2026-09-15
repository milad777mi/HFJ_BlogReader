package com.hfj.blogreader.viewmodel

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hfj.blogreader.data.models.Post
import com.hfj.blogreader.data.models.Comment
import com.hfj.blogreader.data.models.AdData
import com.hfj.blogreader.data.models.EitaaPost
import com.hfj.blogreader.data.models.AdTextItem
import com.hfj.blogreader.data.repository.BlogRepository
import com.hfj.blogreader.utils.FontSizeManager
import com.hfj.blogreader.utils.BlogStats
import com.hfj.blogreader.utils.StatFetcher
import com.hfj.blogreader.utils.LikeManager
import com.hfj.blogreader.utils.CommentManager
import com.hfj.blogreader.utils.AdManager
import com.hfj.blogreader.utils.EitaaApi
import com.hfj.blogreader.utils.AdTextManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val blogRepo = BlogRepository(getApplication())
    private val fontManager = FontSizeManager(getApplication())

    // ⏱️ محدودیت دکمه Refresh: ۱۳ دقیقه
    private val REFRESH_INTERVAL_MS = 13 * 60 * 1000L

    // ⏱️ محدودیت دکمه «نمایش بیشتر»: ۳ دقیقه
    private val LOAD_MORE_INTERVAL_MS = 3 * 60 * 1000L

    private val prefs: SharedPreferences =
        getApplication<Application>().getSharedPreferences("blog_prefs", Context.MODE_PRIVATE)

    val fontScale: StateFlow<Float> = fontManager.fontScale
    fun setFontScale(scale: Float) {
        fontManager.setFontScale(scale)
    }

    private val _allPosts = MutableStateFlow<List<Post>>(emptyList())
    val allPosts: StateFlow<List<Post>> = _allPosts

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore

    private var nextPageUrl: String? = null

    private val _hasMorePosts = MutableStateFlow(true)
    val hasMorePosts: StateFlow<Boolean> = _hasMorePosts

    // 🆕 آیا کاربر می‌تونه دکمه «نمایش بیشتر» رو بزنه؟ (۳ دقیقه)
    private val _canLoadMore = MutableStateFlow(true)
    val canLoadMore: StateFlow<Boolean> = _canLoadMore

    // 🆕 متن پیام برای دکمه «نمایش بیشتر»
    private val _loadMoreMessage = MutableStateFlow<String?>(null)
    val loadMoreMessage: StateFlow<String?> = _loadMoreMessage

    // 🆕 متن پیام برای دکمه Refresh
    private val _refreshMessage = MutableStateFlow<String?>(null)
    val refreshMessage: StateFlow<String?> = _refreshMessage

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

    // Comments
    private val _comments = MutableStateFlow<Map<String, List<Comment>>>(emptyMap())
    val comments: StateFlow<Map<String, List<Comment>>> = _comments

    // Ads
    private val _adData = MutableStateFlow<AdData?>(null)
    val adData: StateFlow<AdData?> = _adData

    private val _eitaaPost = MutableStateFlow<EitaaPost?>(null)
    val eitaaPost: StateFlow<EitaaPost?> = _eitaaPost

    private val _adTextItems = MutableStateFlow<List<AdTextItem>>(emptyList())
    val adTextItems: StateFlow<List<AdTextItem>> = _adTextItems

    // ============================================================
    // لود صفحه اول
    // ============================================================
    fun loadFirstPage() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val (posts, nextUrl) = blogRepo.fetchFirstPage()
                _allPosts.value = posts
                nextPageUrl = nextUrl
                _hasMorePosts.value = nextUrl != null
                _canLoadMore.value = true  // اجازه لود صفحه بعد

                if (posts.isEmpty()) {
                    _errorMessage.value = "⚠️ هیچ پستی یافت نشد"
                }
            } catch (e: Exception) {
                val cached = blogRepo.getCachedPosts()
                if (cached.isNotEmpty()) {
                    _allPosts.value = cached
                } else {
                    _errorMessage.value = "❌ خطا: ${e.message}"
                    _allPosts.value = emptyList()
                }
                e.printStackTrace()
            }
            _isLoading.value = false
        }
    }

    // ============================================================
    // 🆕 لود صفحه بعد (با محدودیت ۳ دقیقه) — فقط با دکمه
    // ============================================================
    fun loadMorePosts() {
        if (_isLoadingMore.value || !_hasMorePosts.value) return
        val url = nextPageUrl ?: return

        // چک محدودیت ۳ دقیقه
        val lastLoadMore = prefs.getLong("last_load_more_time", 0L)
        val elapsed = System.currentTimeMillis() - lastLoadMore

        if (elapsed < LOAD_MORE_INTERVAL_MS) {
            val remainingMs = LOAD_MORE_INTERVAL_MS - elapsed
            val remainingSec = (remainingMs / 1000).toInt()
            val minutes = remainingSec / 60
            val seconds = remainingSec % 60
            val timeText = if (minutes > 0) "$minutes دقيقة و $seconds ثانية" else "$seconds ثانية"
            _loadMoreMessage.value = "⏱️ انتظر $timeText قبل المحاولة مرة أخرى"
            return
        }

        _loadMoreMessage.value = null
        viewModelScope.launch {
            _isLoadingMore.value = true
            try {
                val (newPosts, nextUrl) = blogRepo.fetchNextPage(url)

                val currentIds = _allPosts.value.map { it.id }.toSet()
                val uniqueNewPosts = newPosts.filter { it.id !in currentIds }

                _allPosts.value = _allPosts.value + uniqueNewPosts
                nextPageUrl = nextUrl
                _hasMorePosts.value = nextUrl != null

                // ثبت زمان این لود
                prefs.edit().putLong("last_load_more_time", System.currentTimeMillis()).apply()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            _isLoadingMore.value = false
        }
    }

    // ============================================================
    // 🆕 دکمه Refresh: محدودیت ۱۳ دقیقه
    // ============================================================
    fun forceRefresh() {
        val lastRefresh = prefs.getLong("last_refresh_time", 0L)
        val elapsed = System.currentTimeMillis() - lastRefresh

        if (elapsed < REFRESH_INTERVAL_MS) {
            val remainingMs = REFRESH_INTERVAL_MS - elapsed
            val remainingSec = (remainingMs / 1000).toInt()
            val minutes = remainingSec / 60
            val seconds = remainingSec % 60
            val timeText = if (minutes > 0) "$minutes دقيقة و $seconds ثانية" else "$seconds ثانية"
            _refreshMessage.value = "⏱️ انتظر $timeText قبل التحديث"
            return
        }

        _refreshMessage.value = null
        prefs.edit().putLong("last_refresh_time", System.currentTimeMillis()).apply()
        loadFirstPage()
    }

    fun clearRefreshMessage() {
        _refreshMessage.value = null
    }

    fun clearLoadMoreMessage() {
        _loadMoreMessage.value = null
    }

    // سازگاری با کد قدیم
    fun fetchAllPosts() {
        loadFirstPage()
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

    // Comment functions
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
                    // نظر برای تایید ارسال شد
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Ad functions
    fun loadAdData() {
        viewModelScope.launch {
            try {
                val result = AdManager.getAdData()
                _adData.value = result
            } catch (e: Exception) {
                _adData.value = null
                e.printStackTrace()
            }
        }
    }

    fun loadEitaaPost() {
        viewModelScope.launch {
            try {
                val result = EitaaApi.getLatestPost()
                _eitaaPost.value = result
            } catch (e: Exception) {
                _eitaaPost.value = null
                e.printStackTrace()
            }
        }
    }

    fun loadAdTextItems() {
        viewModelScope.launch {
            try {
                val result = AdTextManager.getAdTextItems()
                _adTextItems.value = result ?: emptyList()
            } catch (e: Exception) {
                _adTextItems.value = emptyList()
                e.printStackTrace()
            }
        }
    }

    init {
        loadFirstPage()

        viewModelScope.launch {
            delay(500)
            loadAdTextItems()

            delay(300)
            loadAdData()

            delay(300)
            loadEitaaPost()
        }
    }
}
