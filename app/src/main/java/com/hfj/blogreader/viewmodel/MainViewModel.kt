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

    // ✅ پیش‌فرض‌ها (میلی‌ثانیه)
    // 0 = بدون محدودیت
    private var APP_OPEN_INTERVAL_MS  = 17 * 60 * 1000L
    private var REFRESH_INTERVAL_MS   = 13 * 60 * 1000L
    private var LOAD_MORE_INTERVAL_MS =  5 * 60 * 1000L

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

    private var nextPageUrl: String? = prefs.getString("next_page_url", null)

    private val _hasMorePosts = MutableStateFlow(!prefs.getString("next_page_url", null).isNullOrBlank())
    val hasMorePosts: StateFlow<Boolean> = _hasMorePosts

    private val _loadMoreMessage = MutableStateFlow<String?>(null)
    val loadMoreMessage: StateFlow<String?> = _loadMoreMessage

    private val _refreshMessage = MutableStateFlow<String?>(null)
    val refreshMessage: StateFlow<String?> = _refreshMessage

    // ✅ جدید: پیام cache
    private val _cacheMessage = MutableStateFlow<String?>(null)
    val cacheMessage: StateFlow<String?> = _cacheMessage

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    val filteredPosts: StateFlow<List<Post>> = _allPosts

    private val _stats = MutableStateFlow(BlogStats())
    val stats: StateFlow<BlogStats> = _stats

    private val _likes = MutableStateFlow<Map<String, Int>>(emptyMap())
    val likes: StateFlow<Map<String, Int>> = _likes

    private val _likedStatus = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val likedStatus: StateFlow<Map<String, Boolean>> = _likedStatus

    private val _comments = MutableStateFlow<Map<String, List<Comment>>>(emptyMap())
    val comments: StateFlow<Map<String, List<Comment>>> = _comments

    private val _adData = MutableStateFlow<AdData?>(null)
    val adData: StateFlow<AdData?> = _adData

    private val _eitaaPost = MutableStateFlow<EitaaPost?>(null)
    val eitaaPost: StateFlow<EitaaPost?> = _eitaaPost

    private val _adTextItems = MutableStateFlow<List<AdTextItem>>(emptyList())
    val adTextItems: StateFlow<List<AdTextItem>> = _adTextItems

    private fun formatRemaining(ms: Long): String {
        val totalSec = (ms / 1000).toInt()
        val min = totalSec / 60
        val sec = totalSec % 60
        return if (min > 0) "$min دقيقة و $sec ثانية" else "$sec ثانية"
    }

    // ✅ اصلاح شد: تبدیل ms به فرمت خوانا (5m, 30s, 1h, No limit)
    private fun formatInterval(ms: Long): String {
        if (ms <= 0L) return "No limit"
        
        val totalSec = ms / 1000
        val hours = totalSec / 3600
        val minutes = (totalSec % 3600) / 60
        val seconds = totalSec % 60
        
        return when {
            hours > 0 -> "${hours}h"
            minutes > 0 -> "${minutes}m"
            else -> "${seconds}s"
        }
    }

    private fun saveNextPageUrl(url: String?) {
        val editor = prefs.edit()
        if (url.isNullOrBlank()) {
            editor.remove("next_page_url")
        } else {
            editor.putString("next_page_url", url)
        }
        editor.apply()
    }

    // ============================================================
    // ✅ لود config از Worker
    // ============================================================
    private fun loadConfigFromServer() {
        viewModelScope.launch {
            try {
                val config = blogRepo.fetchConfig()
                if (config != null) {
                    APP_OPEN_INTERVAL_MS  = config.first
                    REFRESH_INTERVAL_MS   = config.second
                    LOAD_MORE_INTERVAL_MS = config.third

                    prefs.edit()
                        .putLong("cfg_app_open", APP_OPEN_INTERVAL_MS)
                        .putLong("cfg_refresh", REFRESH_INTERVAL_MS)
                        .putLong("cfg_load_more", LOAD_MORE_INTERVAL_MS)
                        .apply()
                } else {
                    loadConfigFromPrefs()
                }
            } catch (e: Exception) {
                loadConfigFromPrefs()
            }
        }
    }

    private fun loadConfigFromPrefs() {
        APP_OPEN_INTERVAL_MS  = prefs.getLong("cfg_app_open",  17 * 60 * 1000L)
        REFRESH_INTERVAL_MS   = prefs.getLong("cfg_refresh",   13 * 60 * 1000L)
        LOAD_MORE_INTERVAL_MS = prefs.getLong("cfg_load_more",  5 * 60 * 1000L)
    }

    // ============================================================
    // 📱 لود اولیه
    // ✅ اگه از Room خوندیم → پیام cache نشون بده
    // ============================================================
    fun loadFirstPage() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            // ✅ چک محدودیت (0 = بدون محدودیت)
            val isNoLimit = APP_OPEN_INTERVAL_MS == 0L
            val lastFetch = prefs.getLong("last_app_open_fetch", 0L)
            val elapsed = System.currentTimeMillis() - lastFetch
            val isCacheValid = !isNoLimit && (elapsed < APP_OPEN_INTERVAL_MS)

            if (isCacheValid) {
                val cached = blogRepo.getCachedPosts()
                if (cached.isNotEmpty()) {
                    _allPosts.value = cached
                    nextPageUrl = prefs.getString("next_page_url", null)
                    _hasMorePosts.value = !nextPageUrl.isNullOrBlank()
                    _isLoading.value = false

                    // ✅ پیام cache: زمان باقی‌مونده
                    val remaining = APP_OPEN_INTERVAL_MS - elapsed
                    val intervalText = formatInterval(APP_OPEN_INTERVAL_MS)
                    val remainingText = formatInterval(remaining)
                    _cacheMessage.value = "📦 From cache | Update: $intervalText | Next in: $remainingText"

                    return@launch
                }
            }

            try {
                val (posts, nextUrl) = blogRepo.fetchFirstPage()
                _allPosts.value = posts
                nextPageUrl = nextUrl
                _hasMorePosts.value = !nextUrl.isNullOrBlank()

                saveNextPageUrl(nextUrl)
                prefs.edit().putLong("last_app_open_fetch", System.currentTimeMillis()).apply()

                if (posts.isEmpty()) {
                    _errorMessage.value = "⚠️ هیچ پستی یافت نشد"
                }
            } catch (e: Exception) {
                val cached = blogRepo.getCachedPosts()
                if (cached.isNotEmpty()) {
                    _allPosts.value = cached
                    nextPageUrl = prefs.getString("next_page_url", null)
                    _hasMorePosts.value = !nextPageUrl.isNullOrBlank()

                    // ✅ پیام cache: به‌خاطر خطا
                    val intervalText = formatInterval(APP_OPEN_INTERVAL_MS)
                    _cacheMessage.value = "📦 From cache (offline) | Update: $intervalText"
                } else {
                    _errorMessage.value = "❌ تعذر الاتصال بالخادم. تحقق من اتصالك بالإنترنت.mms.net.services.errors"
                    _allPosts.value = emptyList()
                }
                e.printStackTrace()
            }
            _isLoading.value = false
        }
    }

    // ============================================================
    // 📥 نمایش بیشتر
    // ============================================================
    fun loadMorePosts() {
        if (_isLoadingMore.value) return
        if (!_hasMorePosts.value) return
        
        val url = prefs.getString("next_page_url", null)
        if (url.isNullOrBlank()) {
            _hasMorePosts.value = false
            return
        }

        if (LOAD_MORE_INTERVAL_MS > 0L) {
            val lastLoadMore = prefs.getLong("last_load_more", 0L)
            val elapsed = System.currentTimeMillis() - lastLoadMore

            if (elapsed < LOAD_MORE_INTERVAL_MS) {
                val remaining = LOAD_MORE_INTERVAL_MS - elapsed
                _loadMoreMessage.value = "⏱️ انتظر ${formatRemaining(remaining)} قبل المحاولة مرة أخرى"
                return
            }
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
                _hasMorePosts.value = !nextUrl.isNullOrBlank()

                saveNextPageUrl(nextUrl)
                prefs.edit().putLong("last_load_more", System.currentTimeMillis()).apply()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            _isLoadingMore.value = false
        }
    }

    // ============================================================
    // 🔄 دکمه Refresh
    // ============================================================
    fun forceRefresh() {
        if (REFRESH_INTERVAL_MS > 0L) {
            val lastRefresh = prefs.getLong("last_manual_refresh", 0L)
            val elapsed = System.currentTimeMillis() - lastRefresh

            if (elapsed < REFRESH_INTERVAL_MS) {
                val remaining = REFRESH_INTERVAL_MS - elapsed
                _refreshMessage.value = "⏱️ انتظر ${formatRemaining(remaining)} قبل التحديث"
                return
            }
        }

        _refreshMessage.value = null
        prefs.edit().putLong("last_manual_refresh", System.currentTimeMillis()).apply()

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val (posts, nextUrl) = blogRepo.fetchFirstPage()
                _allPosts.value = posts
                nextPageUrl = nextUrl
                _hasMorePosts.value = !nextUrl.isNullOrBlank()

                saveNextPageUrl(nextUrl)
            } catch (e: Exception) {
                _errorMessage.value = "❌ تعذر الاتصال بالخادم. تحقق من اتصالك بالإنترنت.mms.net.services.errors"
                e.printStackTrace()
            }
            _isLoading.value = false
        }
    }

    fun clearRefreshMessage() {
        _refreshMessage.value = null
    }

    fun clearLoadMoreMessage() {
        _loadMoreMessage.value = null
    }

    // ✅ جدید: پاک کردن پیام cache
    fun clearCacheMessage() {
        _cacheMessage.value = null
    }

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
                if (success) { }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

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
        loadConfigFromPrefs()
        loadFirstPage()

        viewModelScope.launch {
            delay(500)
            loadAdTextItems()

            delay(300)
            loadAdData()

            delay(300)
            loadEitaaPost()
        }

        viewModelScope.launch {
            delay(2000)
            loadConfigFromServer()
        }
    }
}
