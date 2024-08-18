package com.ashique.qrscanner.custom

import android.util.Log

object Calc {
    fun normalize(x: Float, a: Float, b: Float): Float {
        Log.d("Calc", "${(b - a) * ((x - 0) / (100 - 0)) + a}")
        return ((b - a) * ((x - 0) / (100 - 0))) + a
    }
}