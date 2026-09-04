package com.hfj.blogreader.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hfj.blogreader.data.models.Post
import com.hfj.blogreader.data.repository.BlogRepository
import com.hfj.blogreader.utils.FontSizeManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(
    private val context: Context
) : ViewModel() {

    private val blogRepo = BlogRepository()
    private val fontManager = FontSizeManager(context)

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

    private val _selectedTab = MutableStateFlow("الكل")
    val selectedTab: StateFlow<String> = _selectedTab

    val hashtagTabs: StateFlow<List<String>> = _allPosts.map { posts ->
        val tags = posts.flatMap { it.hashtags }.distinct().sorted()
        listOf("الكل") + tags
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(),
        initialValue = listOf("الكل")
    )

    val filteredPosts: StateFlow<List<Post>> = combine(
        _allPosts,
        _selectedTab
    ) { posts, tab ->
        if (tab == "الكل") posts
        else posts.filter { it.hashtags.any { tag -> tag == tab } }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(),
        initialValue = emptyList()
    )

    fun selectTab(tab: String) {
        _selectedTab.value = tab
    }

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

    init {
        fetchAllPosts()
    }
}
