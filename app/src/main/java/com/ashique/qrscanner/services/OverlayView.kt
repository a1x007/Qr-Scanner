package com.ashique.qrscanner.services

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.ashique.qrscanner.R

class OverlayView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private val path = Path()
    private val overlayPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.overlay_color)
        style = Paint.Style.FILL
    }

    private val borderPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.barcode_overlay_stroke)
        style = Paint.Style.STROKE
        strokeWidth = 5f
        isAntiAlias = true
    }
    private var drawable: Drawable? = null
    private var drawableWidth: Int = 1615
    private var drawableHeight: Int = 600

    init {
        // Initialize your overlay drawable here
        drawable = ContextCompat.getDrawable(context, R.drawable.transformed)
        drawable?.alpha = 30

    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()
        val squareSize = width.coerceAtMost(height) * 0.6f
        val left = (width - squareSize) / 2
        val topOffset = 300f
        val top = (height - squareSize) / 2 - topOffset
        val right = left + squareSize
        val bottom = top + squareSize
        val cornerRadius = 10f

        canvas.apply {
            drawRect(0f, 0f, width, top, overlayPaint)
            drawRect(0f, bottom, width, height, overlayPaint)
            drawRect(0f, top, left, bottom, overlayPaint)
            drawRect(right, top, width, bottom, overlayPaint)


            // Top-left corner
            path.moveTo(left, top + squareSize * 0.1f)
            path.lineTo(left, top + cornerRadius)
            path.arcTo(RectF(left, top, left + 2 * cornerRadius, top + 2 * cornerRadius), 180f, 90f)
            path.lineTo(left + squareSize * 0.1f, top)

            // Top-right corner
            path.moveTo(right - squareSize * 0.1f, top)
            path.lineTo(right - cornerRadius, top)
            path.arcTo(
                RectF(right - 2 * cornerRadius, top, right, top + 2 * cornerRadius),
                270f,
                90f
            )
            path.lineTo(right, top + squareSize * 0.1f)

            // Bottom-right corner
            path.moveTo(right, bottom - squareSize * 0.1f)
            path.lineTo(right, bottom - cornerRadius)
            path.arcTo(
                RectF(right - 2 * cornerRadius, bottom - 2 * cornerRadius, right, bottom),
                0f,
                90f
            )
            path.lineTo(right - squareSize * 0.1f, bottom)

            // Bottom-left corner
            path.moveTo(left + squareSize * 0.1f, bottom)
            path.lineTo(left + cornerRadius, bottom)
            path.arcTo(
                RectF(left, bottom - 2 * cornerRadius, left + 2 * cornerRadius, bottom),
                90f,
                90f
            )
            path.lineTo(left, bottom - squareSize * 0.1f)

            drawPath(path, borderPaint)

            // Draw the drawable on top
            drawable?.apply {
                val drawableLeft = ((left + right - drawableWidth) / 2).toInt()
                val drawableTop = ((top + bottom - drawableHeight) / 2).toInt()
                setBounds(
                    drawableLeft,
                    drawableTop,
                    drawableLeft + drawableWidth + 6,
                    drawableTop + drawableHeight
                )
                draw(canvas)
            }
        }
    }
}
