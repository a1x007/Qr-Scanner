package com.ashique.qrscanner.utils

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.Window
import androidx.core.view.WindowCompat

object EdgeToedge {
    @JvmStatic
    val Context.statusBarHeight: Int
        @SuppressLint("InternalInsetResource")
        get() {
            val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
            return if (resourceId > 0) {
                resources.getDimensionPixelSize(resourceId)
            } else {
                0
            }
        }

    @JvmStatic
    val Context.navigationBarHeight: Int
        @SuppressLint("InternalInsetResource")
        get() {
            val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
            return if (resourceId > 0) {
                resources.getDimensionPixelSize(resourceId)
            } else {
                0
            }
        }


    @JvmStatic
    fun Window.edgeToEdge() {
        WindowCompat.setDecorFitsSystemWindows(this, false)
    }

    @JvmStatic
    fun Context.setInsets(topView: View? = null, bottomView: View? = null){
        topView?.setPadding(0, statusBarHeight, 0, 0)
        bottomView?.setPadding(0, 0, 0, navigationBarHeight)
    }
}