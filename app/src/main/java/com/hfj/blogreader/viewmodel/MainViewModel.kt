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

    // ⏱️ ۱۳ دقیقه برای رفرش هوشمند (طبق درخواستت)
    private val REFRESH_INTERVAL_MS = 13 * 60 * 1000L

    // SharedPreferences برای ثبت زمان آخرین رفرش دستی
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

    // 🆕 برای لود شدن صفحه بعد (پایین لیست)
    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore

    // 🆕 آدرس صفحه بعد (برای اسکرول بی‌نهایت)
    private var nextPageUrl: String? = null

    // 🆕 آیا صفحه بعدی وجود داره؟
    private val _hasMorePosts = MutableStateFlow(true)
    val hasMorePosts: StateFlow<Boolean> = _hasMorePosts

    // 🆕 پیام Pull-to-Refresh (اگه ۱۳ دقیقه نگذشته باشه)
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

    // ✅ Ads
    private val _adData = MutableStateFlow<AdData?>(null)
    val adData: StateFlow<AdData?> = _adData

    // ✅ Eitaa
    private val _eitaaPost = MutableStateFlow<EitaaPost?>(null)
    val eitaaPost: StateFlow<EitaaPost?> = _eitaaPost

    // ✅ AdText
    private val _adTextItems = MutableStateFlow<List<AdTextItem>>(emptyList())
    val adTextItems: StateFlow<List<AdTextItem>> = _adTextItems

    // ============================================================
    // 🆕 لود صفحه اول (برای init و Pull-to-Refresh)
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

                if (posts.isEmpty()) {
                    _errorMessage.value = "⚠️ هیچ پستی یافت نشد"
                }
            } catch (e: Exception) {
                // اگه خطای شبکه داد، از کش بخون
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
    // 🆕 لود صفحه بعد (برای اسکرول بی‌نهایت)
    // ============================================================
    fun loadMorePosts() {
        // اگه در حال لود هستیم یا صفحه بعدی نیست، کاری نکن
        if (_isLoadingMore.value || !_hasMorePosts.value) return
        val url = nextPageUrl ?: return

        viewModelScope.launch {
            _isLoadingMore.value = true
            try {
                val (newPosts, nextUrl) = blogRepo.fetchNextPage(url)

                // اضافه کردن به لیست فعلی (بدون تکرار)
                val currentIds = _allPosts.value.map { it.id }.toSet()
                val uniqueNewPosts = newPosts.filter { it.id !in currentIds }

                _allPosts.value = _allPosts.value + uniqueNewPosts
                nextPageUrl = nextUrl
                _hasMorePosts.value = nextUrl != null
            } catch (e: Exception) {
                e.printStackTrace()
            }
            _isLoadingMore.value = false
        }
    }

    // ============================================================
    // 🆕 Pull-to-Refresh هوشمند (۱۳ دقیقه)
    // ============================================================
    fun smartRefresh() {
        val lastRefresh = prefs.getLong("last_refresh_time", 0L)
        val elapsed = System.currentTimeMillis() - lastRefresh

        if (elapsed < REFRESH_INTERVAL_MS) {
            // ⏱️ ۱۳ دقیقه نگذشته → پیام بده
            val remainingMs = REFRESH_INTERVAL_MS - elapsed
            val remainingMin = (remainingMs / 60000).toInt()
            val remainingSec = ((remainingMs % 60000) / 1000).toInt()
            _refreshMessage.value = "⏱️ لطفاً $remainingMin دقیقه و $remainingSec ثانیه دیگر صبر کنید"
            return
        }

        // ✅ ۱۳ دقیقه گذشته → رفرش کن
        _refreshMessage.value = null
        prefs.edit().putLong("last_refresh_time", System.currentTimeMillis()).apply()
        loadFirstPage()
    }

    // ============================================================
    // ✅ دکمه Refresh توی TopAppBar (بدون محدودیت زمانی)
    // ============================================================
    fun forceRefresh() {
        _refreshMessage.value = null
        prefs.edit().putLong("last_refresh_time", System.currentTimeMillis()).apply()
        loadFirstPage()
    }

    fun clearRefreshMessage() {
        _refreshMessage.value = null
    }

    // ============================================================
    // 🔄 حفظ تابع قدیمی برای سازگاری (اگه جایی صداش زدی)
    // ============================================================
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

    // ✅ Ad functions
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

    // ✅ Eitaa functions
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

    // ✅ AdText functions
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
        // 1. صفحه اول مطالب
        loadFirstPage()

        // 2. کارت‌های تبلیغاتی (بدون تغییر)
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
