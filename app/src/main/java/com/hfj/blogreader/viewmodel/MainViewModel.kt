package com.hfj.blogreader.viewmodel

import android.content.Context
import android.widget.Toast
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

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

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
            try {
                val posts = blogRepo.fetchAllPosts()
                _allPosts.value = posts
                if (posts.isEmpty()) {
                    Toast.makeText(context, "⚠️ هیچ پستی یافت نشد", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "✅ ${posts.size} پست بارگذاری شد", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                _allPosts.value = emptyList()
                Toast.makeText(
                    context,
                    "❌ خطا در دریافت مطالب: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                e.printStackTrace()
            }
            _isLoading.value = false
        }
    }

    init {
        try {
            fetchAllPosts()
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "❌ خطا در شروع برنامه: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
            e.printStackTrace()
        }
    }
}
