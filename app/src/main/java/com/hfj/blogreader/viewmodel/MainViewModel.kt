package com.hfj.blogreader.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hfj.blogreader.data.models.Post
import com.hfj.blogreader.data.repository.BlogRepository
import com.hfj.blogreader.utils.FontSizeManager
import com.hfj.blogreader.utils.BlogStats
import com.hfj.blogreader.utils.StatFetcher
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val blogRepo = BlogRepository()
    private val fontManager = FontSizeManager(getApplication())

    // ---------- Font Size ----------
    val fontScale: StateFlow<Float> = fontManager.fontScale

    fun setFontScale(scale: Float) {
        fontManager.setFontScale(scale)
    }

    // ---------- Posts ----------
    private val _allPosts = MutableStateFlow<List<Post>>(emptyList())
    val allPosts: StateFlow<List<Post>> = _allPosts

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    // بدون هشتگ: فقط همه پست‌ها را نمایش بده
    val filteredPosts: StateFlow<List<Post>> = _allPosts

    // ---------- Stats ----------
    private val _stats = MutableStateFlow(BlogStats())
    val stats: StateFlow<BlogStats> = _stats

    // ---------- Functions ----------

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

    fun loadStats(context: android.content.Context) {
        viewModelScope.launch {
            try {
                val result = StatFetcher.fetchStats(context)
                _stats.value = result
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    init {
        fetchAllPosts()
    }
}
