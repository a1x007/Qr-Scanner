package com.ashique.qrscanner

import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.chaquo.python.android.PyApplication

class MyApp : PyApplication() {
    override fun onCreate() {
        super.onCreate()

        // Initialize Python environment
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }
    }
}
