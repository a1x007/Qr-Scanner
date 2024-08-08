package com.ashique.qrscanner.colorpicker

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Shader
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import com.ashique.qrscanner.R
import com.ashique.qrscanner.helper.BitmapHelper.saveBitmapToFile
import com.ashique.qrscanner.helper.Extensions.dp
import kotlin.math.floor
import kotlin.math.max

abstract class ColorSlider(context: Context, attributeSet: AttributeSet?) :
    View(context, attributeSet) {

    constructor(context: Context) : this(context, null)

    protected val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    protected val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.RED
    }

   private var isPatternBitmapCreated = false

 //  private val patternBitmap: Bitmap by lazy {   BitmapFactory.decodeResource(context.resources, R.drawable.transparent) }
    private val patternPaint = Paint().apply {
     // shader = BitmapShader(patternBitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
    }

    protected var isAlpha = false

    protected var widthF = 0f
    protected var heightF = 0f

    protected var heightHalf = 0f

    protected var drawingStart = 0f
    protected var drawingTop = 0f

    protected var strokeWidthHalf = 0f

    protected var circleX = 0f
    protected var circleY = 0f

    protected var circleXFactor = 0f
    protected var circleYFactor = 0f

    protected var circleColor: Int = Color.TRANSPARENT
    var circleSize = dp(10)
    var thickness = dp(0)

    protected var isFirstTimeLaying = true
    protected var isRestoredState = false

    private var isWrapContent = false

    var lineStrokeCap = Paint.Cap.ROUND
        set(value) {
            field = value
            linePaint.strokeCap = field
            requestLayout()
        }

    var strokeColor = Color.WHITE
        set(value) {
            field = value
            invalidate()
        }

    var strokeSize = dp(2)
        set(value) {
            field = value
            invalidate()
        }

    private var defaultPaddingVertical = 0//resources.getDimensionPixelOffset(R.dimen.colorSlider_padding)
    private var wrapContentSize = 80//resources.getDimensionPixelOffset(R.dimen.colorSlider_wrapContent_size)

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


    }


    private fun initializePatternPaint() {
        if (!isPatternBitmapCreated) {
            patternPaint.shader = BitmapShader(createPatternBitmap(), Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
            isPatternBitmapCreated = true
        }
    }


    protected open fun changePositionOfCircle(ex: Float, ey: Float) {
        val validCircleX = if (drawingStart <= widthF) ex.coerceIn(drawingStart, widthF) else ex
        val validCircleY = if (drawingTop <= heightF) ey.coerceIn(drawingTop, heightF) else ey

        circleX = floor(validCircleX)
        circleY = floor(validCircleY)

        onCirclePositionChanged(circleX, circleY)
    }

    /**
     * Called when position of indicator changes in slider or picker.
     * @param circlePositionX Position of indicator in x axis.
     * @param circlePositionY Position of indicator in y axis.
     */
    protected open fun onCirclePositionChanged(circlePositionX: Float, circlePositionY: Float) {

    }



    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event?.let { e ->
            return when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    changePositionOfCircle(e.x, e.y)
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    changePositionOfCircle(e.x, e.y)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    changePositionOfCircle(e.x, e.y)
                    false
                }
                else -> {
                    false
                }
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        if (w != oldw || h != oldh) {

            calculateBounds(w.toFloat(), h.toFloat())

            initializeSliderPaint()
            initializePatternPaint()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val measureWidth = MeasureSpec.getSize(widthMeasureSpec)
        val measureHeight = MeasureSpec.getSize(heightMeasureSpec)

        isWrapContent = false

        val finalHeight = when (MeasureSpec.getMode(heightMeasureSpec)) {
            MeasureSpec.EXACTLY -> {
                measureHeight
            }
            MeasureSpec.AT_MOST, MeasureSpec.UNSPECIFIED -> {
                isWrapContent = true
                wrapContentSize
            }
            else -> {
                suggestedMinimumHeight
            }
        }

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

    /**
     * Calculates bounds of drawing elements.
     * @param targetWidth limiting width of drawings, often width of the view is supplied.
     * @param targetHeight limiting height of drawings, often height of the view is supplied.
     */
    protected open fun calculateBounds(targetWidth: Float, targetHeight: Float) {
        val fx = (circleX - drawingStart) / (widthF - drawingStart)
        val fy = (circleY - drawingTop) / (heightF - drawingTop)

        heightF = targetHeight - paddingBottom - paddingTop

        if (isWrapContent) {
            heightF -= (defaultPaddingVertical * 2f)
        }

        heightHalf = heightF * 0.5f
        linePaint.strokeWidth = if (thickness == 0f) heightHalf else thickness

        widthF = targetWidth //- paddingEnd - heightHalf

        drawingStart = 0f //heightHalf + paddingStart
        drawingTop = heightHalf + paddingTop

        if (isWrapContent) {
            drawingTop += defaultPaddingVertical
        }

        // If it's first layout pass then set indicator to be at top-end of the color picker amd sliders.
        if (isFirstTimeLaying) {
            isFirstTimeLaying = false
            circleX = widthF
            circleY = drawingTop
        } else if (isRestoredState) {
            // Use the factors that are returned in 'onRestoreInstanceState' to correctly
            // calculate the position of indicators in case of screen rotation.
            circleX = ((widthF - drawingStart) * circleXFactor) + drawingStart
            circleY = ((heightF - drawingTop) * circleYFactor) + drawingTop

            circleXFactor = 0f
            circleYFactor = 0f

            isRestoredState = false
        } else {
            // Calculate position of indicator when a size change happens on view.
            circleX = ((widthF - drawingStart) * fx) + drawingStart
            circleY = ((heightF - drawingTop) * fy) + drawingTop
        }

        strokeWidthHalf = if (linePaint.strokeCap != Paint.Cap.BUTT) {
            // If paint cap is not BUTT then add half the width of stroke at start and end of line.
            linePaint.strokeWidth * 0.5f
        } else {
            0f
        }

    }

    override fun onDraw(canvas: Canvas) {

        // Draw transparent pattern
        if (isAlpha) { canvas.drawRect(drawingStart, drawingTop - circleSize - 0.5f , widthF, drawingTop + circleSize + 4, patternPaint) }


        canvas.drawLine(
            drawingStart,
            drawingTop,
            widthF,
            drawingTop,
            linePaint
        )


        canvas.drawCircle(
            circleX,
            drawingTop,
            circleSize,
            circlePaint.apply {
                color = strokeColor
            })

        canvas.drawCircle(
            circleX,
            drawingTop,
            circleSize - strokeSize,
            circlePaint.apply {
                color = circleColor
            })

    }

    private fun createPatternBitmap(): Bitmap {
        val density = context.resources.displayMetrics.density
        val smallSquareSizeDp = 7 // Size of each square in dp
        val numColumns = 4
        val numRows = 4
        val smallSquareSizePx = (smallSquareSizeDp * density).toInt()
        val bitmapSize = smallSquareSizePx * numColumns
        val bitmap = Bitmap.createBitmap(bitmapSize, bitmapSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()

        // Draw the checkerboard pattern
        paint.color = "#50ffffff".toColorInt() // Color for the checkerboard squares
        for (row in 0 until numRows) {
            for (col in 0 until numColumns) {
                if ((row + col) % 2 == 0) {
                    canvas.drawRect(
                        (col * smallSquareSizePx).toFloat(),
                        (row * smallSquareSizePx).toFloat(),
                        ((col + 1) * smallSquareSizePx).toFloat(),
                        ((row + 1) * smallSquareSizePx).toFloat(),
                        paint
                    )
                }
            }
        }

        Log.i("ColorSlider", "createPatternBitmap: pattern bitmap created.")
      //  context.saveBitmapToFile(bitmap,"transparent")
        return bitmap
    }



    override fun onSaveInstanceState(): Parcelable? {
        return Bundle().apply {
            // Save current position of circle as factors to later restore it's state.
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
    }

    companion object {
        private const val CIRCLE_X_KEY = "circleX"
        private const val CIRCLE_Y_KEY = "circleY"
        private const val STATE_KEY = "p"
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

    }

}