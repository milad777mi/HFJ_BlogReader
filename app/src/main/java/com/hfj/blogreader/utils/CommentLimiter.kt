package com.hfj.blogreader.utils

import android.content.Context
import java.util.concurrent.TimeUnit

object CommentLimiter {

    private const val PREF_NAME = "comment_limiter"
    private const val KEY_DAILY_COUNT = "daily_count"
    private const val KEY_FIRST_COMMENT_TIME = "first_comment_time"

    // محدودیت‌ها
    private const val MAX_COMMENTS_PER_DAY = 5
    private const val TIME_LIMIT_HOURS = 24

    fun canComment(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val count = prefs.getInt(KEY_DAILY_COUNT, 0)
        val firstTime = prefs.getLong(KEY_FIRST_COMMENT_TIME, 0)
        val now = System.currentTimeMillis()

        if (firstTime > 0 && now - firstTime > TimeUnit.HOURS.toMillis(TIME_LIMIT_HOURS)) {
            reset(context)
            return true
        }

        return count < MAX_COMMENTS_PER_DAY
    }

    fun canCommentOnPost(context: Context, postId: String): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val key = "post_$postId"
        val lastCommentTime = prefs.getLong(key, 0)
        val now = System.currentTimeMillis()

        return now - lastCommentTime > TimeUnit.HOURS.toMillis(TIME_LIMIT_HOURS)
    }

    fun registerComment(context: Context, postId: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()

        prefs.edit().putLong("post_$postId", now).apply()

        val dailyCount = prefs.getInt(KEY_DAILY_COUNT, 0)
        val firstTime = prefs.getLong(KEY_FIRST_COMMENT_TIME, 0)

        if (firstTime == 0L) {
            prefs.edit().putLong(KEY_FIRST_COMMENT_TIME, now).apply()
        }

        prefs.edit().putInt(KEY_DAILY_COUNT, dailyCount + 1).apply()
    }

    fun getRemainingComments(context: Context): Int {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val count = prefs.getInt(KEY_DAILY_COUNT, 0)
        val firstTime = prefs.getLong(KEY_FIRST_COMMENT_TIME, 0)
        val now = System.currentTimeMillis()

        if (firstTime > 0 && now - firstTime > TimeUnit.HOURS.toMillis(TIME_LIMIT_HOURS)) {
            reset(context)
            return MAX_COMMENTS_PER_DAY
        }

        return (MAX_COMMENTS_PER_DAY - count).coerceAtLeast(0)
    }

    fun getRemainingTime(context: Context): Long {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val firstTime = prefs.getLong(KEY_FIRST_COMMENT_TIME, 0)
        val now = System.currentTimeMillis()

        if (firstTime == 0L) return 0L
        val elapsed = now - firstTime
        val remaining = TimeUnit.HOURS.toMillis(TIME_LIMIT_HOURS) - elapsed
        return remaining.coerceAtLeast(0)
    }

    fun reset(context: Context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }

    fun formatRemainingTime(millis: Long): String {
        if (millis <= 0) return ""

        val hours = TimeUnit.MILLISECONDS.toHours(millis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60

        return when {
            hours > 0 -> "$hours ساعة و $minutes دقيقة"
            minutes > 0 -> "$minutes دقيقة"
            else -> "أقل من دقيقة"
        }
    }
}
