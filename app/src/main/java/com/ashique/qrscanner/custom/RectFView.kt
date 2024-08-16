package com.ashique.qrscanner.custom

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class RectFView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {


    private val paint: Paint = Paint().apply {
        color = 0xFF0000FF.toInt() // Blue color
        strokeWidth = 5f
        style = Paint.Style.FILL
    }

    private var path: Path = Path()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawPath(path, paint)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // Update the path based on the new width and height
        path.reset()
        path.addRect(0f, 0f, w.toFloat(), h.toFloat(), Path.Direction.CW)
    }

    fun setPath(newPath: Path) {
        this.path = newPath
        invalidate()
    }
}