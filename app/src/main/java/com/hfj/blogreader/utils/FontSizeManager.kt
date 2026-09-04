package com.hfj.blogreader.utils

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FontSizeManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    private val KEY_FONT_SIZE = "font_size_scale"
    private val DEFAULT_SIZE = 1.0f

    private val _fontScale = MutableStateFlow(getSavedFontScale())
    val fontScale: StateFlow<Float> = _fontScale

    fun setFontScale(scale: Float) {
        val clamped = scale.coerceIn(0.7f, 1.8f)
        _fontScale.value = clamped
        prefs.edit().putFloat(KEY_FONT_SIZE, clamped).apply()
    }

    private fun getSavedFontScale(): Float {
        return prefs.getFloat(KEY_FONT_SIZE, DEFAULT_SIZE)
    }
}
