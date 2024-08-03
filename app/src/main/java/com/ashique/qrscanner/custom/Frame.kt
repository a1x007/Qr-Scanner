package com.ashique.qrscanner.custom

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.annotation.ColorInt
import androidx.core.content.withStyledAttributes
import com.ashique.qrscanner.R

class Frame @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var cornerRadius: Float = 0f
    private var borderWidth: Float = 0f
    private var borderColor: Int = Color.BLACK
    private var backgroundColor: Int = Color.TRANSPARENT

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rectF = RectF()
    private val path = Path()

    init {
        context.withStyledAttributes(attrs, R.styleable.Frame) {
            cornerRadius = getDimension(R.styleable.Frame_cornerRadius, 0f)
            borderWidth = getDimension(R.styleable.Frame_borderWidth, 0f)
            borderColor = getColor(R.styleable.Frame_borderColor, Color.BLACK)
            backgroundColor = getColor(R.styleable.Frame_backgroundColor, Color.TRANSPARENT)
        }

        borderPaint.style = Paint.Style.STROKE
        borderPaint.strokeWidth = borderWidth
        borderPaint.color = borderColor

        backgroundPaint.style = Paint.Style.FILL
        backgroundPaint.color = backgroundColor

        setWillNotDraw(false)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        rectF.set(
            borderWidth / 2,
            borderWidth / 2,
            width.toFloat() - borderWidth / 2,
            height.toFloat() - borderWidth / 2
        )
        path.reset()
        path.addRoundRect(rectF, cornerRadius, cornerRadius, Path.Direction.CW)
        canvas.drawPath(path, backgroundPaint)
        canvas.drawPath(path, borderPaint)
    }

    override fun dispatchDraw(canvas: Canvas) {
        canvas.save()
        canvas.clipPath(path)
        super.dispatchDraw(canvas)
        canvas.restore()
    }

    fun setCornerRadius(radius: Float) {
        cornerRadius = radius
        invalidate()
    }

    fun setBorderWidth(width: Float) {
        borderWidth = width
        borderPaint.strokeWidth = borderWidth
        invalidate()
    }

    fun setBorderColor(@ColorInt color: Int) {
        borderColor = color
        borderPaint.color = borderColor
        invalidate()
    }

    fun setCustomBackgroundColor(@ColorInt color: Int) {
        backgroundColor = color
        backgroundPaint.color = backgroundColor
        invalidate()
    }
}
