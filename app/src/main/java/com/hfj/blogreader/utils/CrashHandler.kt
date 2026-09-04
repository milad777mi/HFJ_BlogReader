package com.hfj.blogreader.utils

import android.content.Context
import android.os.Build
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.*

class CrashHandler(private val context: Context) : Thread.UncaughtExceptionHandler {

    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            val stackTrace = throwable.stackTrace.joinToString("\n")
            val message = """
                =======================================
                Crash Report
                =======================================
                App Name: HFJ_BlogReader
                Version: 1.0.0
                Android Version: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})
                Device: ${Build.MANUFACTURER} ${Build.MODEL}
                Time: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}
                Thread: ${thread.name}
                Exception: ${throwable.javaClass.name}
                Message: ${throwable.message}
                StackTrace:
                $stackTrace
                =======================================
            """.trimIndent()

            Log.e("CrashHandler", message)

            // تلاش برای ذخیره در حافظه خارجی (Downloads)
            val externalFile = getExternalFile()
            if (externalFile != null) {
                FileWriter(externalFile, true).use { writer ->
                    PrintWriter(writer).use { printWriter ->
                        printWriter.println(message)
                    }
                }
            } else {
                // اگر حافظه خارجی در دسترس نبود، در حافظه داخلی ذخیره کن
                val internalFile = File(context.filesDir, "crash_log.txt")
                FileWriter(internalFile, true).use { writer ->
                    PrintWriter(writer).use { printWriter ->
                        printWriter.println(message)
                    }
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }

        defaultHandler?.uncaughtException(thread, throwable)
    }

    private fun getExternalFile(): File? {
        return try {
            // بررسی در دسترس بودن حافظه خارجی
            if (Environment.getExternalStorageState() != Environment.MEDIA_MOUNTED) {
                return null
            }

            val directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!directory.exists()) {
                directory.mkdirs()
            }
            File(directory, "crash_log.txt")
        } catch (e: Exception) {
            null
        }
    }

    fun getCrashLog(): String? {
        // ابتدا فایل را در حافظه خارجی جستجو کن
        val externalFile = getExternalFile()
        if (externalFile != null && externalFile.exists()) {
            return externalFile.readText()
        }

        // اگر نبود، در حافظه داخلی جستجو کن
        val internalFile = File(context.filesDir, "crash_log.txt")
        return if (internalFile.exists()) {
            internalFile.readText()
        } else {
            null
        }
    }

    fun clearCrashLog() {
        val externalFile = getExternalFile()
        if (externalFile != null && externalFile.exists()) {
            externalFile.delete()
        }

        val internalFile = File(context.filesDir, "crash_log.txt")
        if (internalFile.exists()) {
            internalFile.delete()
        }
    }
}
