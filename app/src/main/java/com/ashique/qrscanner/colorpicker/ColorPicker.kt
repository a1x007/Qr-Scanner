package com.ashique.qrscanner.colorpicker

import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import com.ashique.qrscanner.helper.Extensions.dp
import kotlin.math.max
import kotlin.math.min

class ColorPicker(context: Context, attributeSet: AttributeSet?) :
    ColorSlider(context, attributeSet) {

    constructor(context: Context) : this(context, null)

    private lateinit var colorShader: LinearGradient
    private lateinit var darknessShader: LinearGradient

    private val hsvArray = FloatArray(3)

    var hue = 30
        set(value) {
            require(value.toFloat() in 0f..360f) { "Hue value should be between 0 and 360" }
            if (field != value) {
                field = value
                initializeSliderPaint()
                calculateColor(circleX, circleY)
                invalidate()
            }
        }

    var circleIndicatorRadius = dp(0)
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }

    var alphaSliderView: ColorAlphaSlider? = null
        set(value) {
            field = value
            alphaSliderView?.selectedColor = colorWithFullAlpha
            alphaSliderView?.setOnAlphaChangedListener {
                alphaValue = (255 * it).toInt().coerceIn(0, 255)
                callListeners()
            }
        }

    var hueSliderView: HueSlider? = null
        set(value) {
            field = value
            hueSliderView?.let { slider ->
                hue = slider.hue.toInt()
                slider.setOnHueChangedListener { newHue, _ ->
                    if (newHue in 0f..360f) hue = newHue.toInt()
                }
            }
        }

    private var colorWithFullAlpha = Color.RED

    var color: Int
        get() = Color.HSVToColor(alphaValue, hsvArray)
        private set(value) {}

    private var alphaValue = 255
    private var onColorChanged: ((color: Int) -> Unit)? = null
    private var onColorChangedListener: OnColorChangedListener? = null
    private val defaultSize = dp(320).toInt()

    init {
        linePaint.style = Paint.Style.FILL
    }

    override fun onCirclePositionChanged(circlePositionX: Float, circlePositionY: Float) {
        calculateColor(circlePositionX, circlePositionY)
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val finalWidth = MeasureSpec.getSize(widthMeasureSpec).let { measuredWidth ->
            when (MeasureSpec.getMode(widthMeasureSpec)) {
                MeasureSpec.EXACTLY -> measuredWidth
                MeasureSpec.AT_MOST -> min(defaultSize, measuredWidth)
                else -> defaultSize
            }
        }

        val finalHeight = MeasureSpec.getSize(heightMeasureSpec).let { measuredHeight ->
            when (MeasureSpec.getMode(heightMeasureSpec)) {
                MeasureSpec.EXACTLY -> measuredHeight
                MeasureSpec.AT_MOST -> min(defaultSize, measuredHeight)
                else -> defaultSize
            }
        }

        setMeasuredDimension(
            max(finalWidth, suggestedMinimumWidth),
            max(finalHeight, suggestedMinimumHeight)
        )
    }

    override fun calculateBounds(targetWidth: Float, targetHeight: Float) {
        val fx = (circleX - drawingStart) / (widthF - drawingStart)
        val fy = (circleY - drawingTop) / (heightF - drawingTop)

        widthF = targetWidth - paddingEnd - circleIndicatorRadius
        heightF = targetHeight - paddingBottom - circleIndicatorRadius

        drawingStart = paddingStart + circleIndicatorRadius
        drawingTop = paddingTop + circleIndicatorRadius

        circleX = if (isFirstTimeLaying) widthF else ((widthF - drawingStart) * fx) + drawingStart
        circleY = if (isFirstTimeLaying) drawingTop else ((heightF - drawingTop) * fy) + drawingTop

        if (isFirstTimeLaying) {
            isFirstTimeLaying = false
        } else if (isRestoredState) {
            circleXFactor = 0f
            circleYFactor = 0f
            isRestoredState = false
        }
    }

    private fun calculateColor(ex: Float, ey: Float) {
        hsvArray[0] = hue.toFloat()
        hsvArray[1] = if (isFirstTimeLaying) 1f else (ex - drawingStart) / (widthF - drawingStart)
        hsvArray[2] = if (isFirstTimeLaying) 1f else 1f - ((ey - drawingTop) / (heightF - drawingTop))

        colorWithFullAlpha = Color.HSVToColor(hsvArray)
        alphaSliderView?.selectedColor = colorWithFullAlpha

        callListeners()
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawRect(drawingStart, drawingTop, widthF, heightF, linePaint.apply {
            shader = colorShader
        })
        canvas.drawRect(drawingStart, drawingTop, widthF, heightF, linePaint.apply {
            shader = darknessShader
        })
        drawCircleIndicator(canvas)
    }

    private fun drawCircleIndicator(canvas: Canvas) {
        canvas.drawCircle(circleX, circleY, circleSize, circlePaint.apply {
            color = strokeColor
        })
        canvas.drawCircle(circleX, circleY, circleSize - strokeSize, circlePaint.apply {
            color = colorWithFullAlpha
        })
    }

    override fun initializeSliderPaint() {
        hsvArray[0] = hue.toFloat()
        colorShader = LinearGradient(
            drawingStart, 0f, widthF, 0f,
            Color.WHITE, Color.HSVToColor(hsvArray),
            Shader.TileMode.MIRROR
        )
        darknessShader = LinearGradient(
            0f, drawingTop, 0f, heightF,
            Color.TRANSPARENT, Color.BLACK,
            Shader.TileMode.MIRROR
        )
        calculateColor(circleX, circleY)
    }

    override fun onSaveInstanceState(): Parcelable {
        return (super.onSaveInstanceState() as Bundle).apply {
            putInt(HUE_KEY, hue)
            putInt(ALPHA_KEY, alphaValue)
        }
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        (state as? Bundle)?.let { bundle ->
            isFirstTimeLaying = false
            alphaValue = bundle.getInt(ALPHA_KEY)
            hue = bundle.getInt(HUE_KEY)
        }
        super.onRestoreInstanceState(state)
    }

    fun setOnColorChangedListener(listener: OnColorChangedListener) {
        this.onColorChangedListener = listener
        callListeners()
    }

    fun setOnColorChangedListener(listener: (color: Int) -> Unit) {
        this.onColorChanged = listener
        callListeners()
    }

    private fun callListeners() {
        val currentColor = Color.HSVToColor(alphaValue, hsvArray)
        onColorChanged?.invoke(currentColor)
        onColorChangedListener?.onColorChanged(currentColor)
    }

    interface OnColorChangedListener {
        fun onColorChanged(color: Int)
    }

    companion object {
        private const val HUE_KEY = "hue"
        private const val ALPHA_KEY = "alpha"
    }
}
