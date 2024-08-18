package com.ashique.qrscanner.custom

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.isseiaoki.simplecropview.CropImageView

class CropImageView : CropImageView {


    var onScanRequested: (() -> Unit)? = null

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event?.let {
            when (it.action) {
                MotionEvent.ACTION_UP -> {
                    // Trigger the scan when the user lifts their finger
                    onScanRequested?.invoke()
                }

                else -> {

                }
            }
        }
        return super.onTouchEvent(event)
    }
}

