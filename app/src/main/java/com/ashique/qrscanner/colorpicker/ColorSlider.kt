package com.ashique.qrscanner.colorpicker

import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.ashique.qrscanner.R
import com.ashique.qrscanner.helper.BitmapHelper.createPatternBitmap
import com.ashique.qrscanner.utils.Extensions.dp
import kotlin.math.floor
import kotlin.math.max

abstract class ColorSlider(context: Context, attributeSet: AttributeSet?) :
    View(context, attributeSet) {

    constructor(context: Context) : this(context, null)

    val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = dp(2)
        color = Color.WHITE
    }

    val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private var isPatternBitmapCreated = false
    private var patternBitmap: Bitmap? = null
    private val patternPaint = Paint()

    var isAlpha = false
    var widthF = 0f
    var heightF = 0f
    private var heightHalf = 0f
    var drawingStart = 0f
    var drawingTop = 0f
    private var strokeWidthHalf = 0f
    var circleX = 0f
    var circleY = 0f
    var circleXFactor = 0f
    var circleYFactor = 0f
    var circleColor: Int = Color.TRANSPARENT
    var circleSize = dp(10)
    var thickness = dp(0)
    var isFirstTimeLaying = true
    var isRestoredState = false
    private var isWrapContent = false
    private val defaultPaddingVertical = 0
    private val wrapContentSize = 80

    var lineStrokeCap = Paint.Cap.ROUND
        set(value) {
            field = value
            linePaint.strokeCap = field
            requestLayout()
        }

    var strokeColor = Color.WHITE
        set(value) {
            field = value
            linePaint.color = field
            invalidate()
        }

    var strokeSize = dp(2)
        set(value) {
            field = value
            linePaint.strokeWidth = field
            invalidate()
        }

    init {
        context.theme.obtainStyledAttributes(attributeSet, R.styleable.ColorSlider, 0, 0).apply {
            try {
                lineStrokeCap = Paint.Cap.entries.toTypedArray()[getInt(
                    R.styleable.ColorSlider_sliderBarStrokeCap,
                    Paint.Cap.ROUND.ordinal
                )]

                strokeSize = getDimension(R.styleable.ColorSlider_sliderStrokeSize, strokeSize)

                strokeColor = getColor(R.styleable.ColorSlider_sliderStrokeColor, strokeColor)

                circleSize = getDimension(R.styleable.ColorSlider_sliderThumbSize, circleSize)

                thickness = getDimension(R.styleable.ColorSlider_sliderThickness, thickness)
            } finally {
                recycle()
            }
        }

        initializePatternPaint()
    }

    private fun initializePatternPaint() {
        if (!isPatternBitmapCreated) {
            patternBitmap = context.createPatternBitmap()
            patternPaint.shader = BitmapShader(patternBitmap!!, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
            isPatternBitmapCreated = true
        }
    }

    protected open fun changePositionOfCircle(ex: Float, ey: Float) {
        val validCircleX = ex.coerceIn(drawingStart, widthF)
        val validCircleY = ey.coerceIn(drawingTop, heightF)
        circleX = floor(validCircleX)
        circleY = floor(validCircleY)
        onCirclePositionChanged(circleX, circleY)
    }

    protected open fun onCirclePositionChanged(circlePositionX: Float, circlePositionY: Float) {
        // Subclasses should override this method
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event?.let { e ->
            return when (e.actionMasked) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                    changePositionOfCircle(e.x, e.y)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    changePositionOfCircle(e.x, e.y)
                    false
                }
                else -> super.onTouchEvent(event)
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        if (w != oldw || h != oldh) {
            calculateBounds(w.toFloat(), h.toFloat())
            initializeSliderPaint()
         //   initializePatternPaint()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val measureWidth = MeasureSpec.getSize(widthMeasureSpec)
        val measureHeight = MeasureSpec.getSize(heightMeasureSpec)

        isWrapContent = MeasureSpec.getMode(heightMeasureSpec) != MeasureSpec.EXACTLY

        val finalHeight = if (isWrapContent) wrapContentSize else measureHeight

        setMeasuredDimension(
            max(measureWidth, suggestedMinimumWidth),
            max(finalHeight, suggestedMinimumHeight)
        )
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        if (changed) {
            calculateBounds(width.toFloat(), height.toFloat())
            initializeSliderPaint()
        }
    }

    protected open fun calculateBounds(targetWidth: Float, targetHeight: Float) {
        val fx = (circleX - drawingStart) / (widthF - drawingStart)
        val fy = (circleY - drawingTop) / (heightF - drawingTop)

        heightF = targetHeight - paddingBottom - paddingTop

        if (isWrapContent) {
            heightF -= (defaultPaddingVertical * 2f)
        }

        heightHalf = heightF * 0.5f
        linePaint.strokeWidth = if (thickness == 0f) heightHalf else thickness
        widthF = targetWidth
        drawingStart = 0f
        drawingTop = heightHalf + paddingTop

        if (isWrapContent) {
            drawingTop += defaultPaddingVertical
        }

        if (isFirstTimeLaying) {
            isFirstTimeLaying = false
            circleX = widthF
            circleY = drawingTop
        } else if (isRestoredState) {
            circleX = ((widthF - drawingStart) * circleXFactor) + drawingStart
            circleY = ((heightF - drawingTop) * circleYFactor) + drawingTop
            circleXFactor = 0f
            circleYFactor = 0f
            isRestoredState = false
        } else {
            circleX = ((widthF - drawingStart) * fx) + drawingStart
            circleY = ((heightF - drawingTop) * fy) + drawingTop
        }

        strokeWidthHalf = if (linePaint.strokeCap != Paint.Cap.BUTT) {
            linePaint.strokeWidth * 0.5f
        } else {
            0f
        }
    }

    override fun onDraw(canvas: Canvas) {
        // Draw transparent pattern
        if (isAlpha) {
            canvas.drawRect(drawingStart, drawingTop - circleSize - 0.5f, widthF, drawingTop + circleSize + 4, patternPaint)
        }

        // Draw line
        canvas.drawLine(drawingStart, drawingTop, widthF, drawingTop, linePaint)

        // Draw circles with caching of Paint color
        circlePaint.color = strokeColor
        canvas.drawCircle(circleX, drawingTop, circleSize, circlePaint)

        circlePaint.color = circleColor
        canvas.drawCircle(circleX, drawingTop, circleSize - strokeSize, circlePaint)
    }

    override fun onSaveInstanceState(): Parcelable? {
        return Bundle().apply {
            putFloat(CIRCLE_X_KEY, (circleX - drawingStart) / (widthF - drawingStart))
            putFloat(CIRCLE_Y_KEY, (circleY - drawingTop) / (heightF - drawingTop))
            putParcelable(STATE_KEY, super.onSaveInstanceState())
        }
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        (state as? Bundle)?.apply {
            circleXFactor = getFloat(CIRCLE_X_KEY)
            circleYFactor = getFloat(CIRCLE_Y_KEY)
            isFirstTimeLaying = false
            isRestoredState = true
            super.onRestoreInstanceState(getParcelable(STATE_KEY))
            return
        }
        super.onRestoreInstanceState(state)
    }

    open fun initializeSliderPaint() {
        // Subclasses should override this method
    }

    companion object {
        private const val CIRCLE_X_KEY = "circleX"
        private const val CIRCLE_Y_KEY = "circleY"
        private const val STATE_KEY = "p"
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        patternBitmap?.recycle()
        patternBitmap = null
    }
}
