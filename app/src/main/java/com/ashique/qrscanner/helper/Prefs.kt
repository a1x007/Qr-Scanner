package com.ashique.qrscanner.helper

import android.content.Context
import android.content.SharedPreferences

object Prefs {
    private const val PREFS_NAME = "qr-scanner"
    private const val USE_ZXING = "use-zxing"
    lateinit var sharedPreferences: SharedPreferences private set


    fun initialize(context: Context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun setZxing(value: Boolean){
        with(sharedPreferences.edit()) {
            putBoolean(USE_ZXING, value)
            apply()
        }
    }

    fun useZxing(): Boolean {
        return sharedPreferences.getBoolean(USE_ZXING, true)
    }

}