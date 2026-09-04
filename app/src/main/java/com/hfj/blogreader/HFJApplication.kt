package com.hfj.blogreader

import android.app.Application
import com.hfj.blogreader.utils.CrashHandler

class HFJApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        val crashHandler = CrashHandler(this)
        Thread.setDefaultUncaughtExceptionHandler(crashHandler)
    }
}
