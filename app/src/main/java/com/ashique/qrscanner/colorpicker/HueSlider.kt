package com.ashique.qrscanner.colorpicker

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import com.ashique.qrscanner.R
import kotlin.math.floor

class HueSlider(context: Context, attributeSet: AttributeSet?) :
    ColorSlider(context, attributeSet) {

    constructor(context: Context) : this(context, null)

    private var hsvHolder = FloatArray(3)

    private lateinit var hueBitmapShader: BitmapShader

    // Load the bitmap only once and recycle when not needed
    private val hueBitmap: Bitmap by lazy {
        BitmapFactory.decodeResource(
            resources,
            R.drawable.full_hue_bitmap,
            BitmapFactory.Options().apply {
                inScaled = false
            }
        )
    }

    private val hueMatrix = Matrix()

    private var onHueChanged: ((hue: Float, argbColor: Int) -> Unit)? = null
    private var onHueChangedListener: OnHueChangedListener? = null

    val hue: Float
        get() = hsvHolder[0]

    override fun onCirclePositionChanged(circlePositionX: Float, circlePositionY: Float) {
        circleColor = calculateColorAt(circlePositionX)

        callListeners(hsvHolder[0], circleColor)

        invalidate()
    }

    private fun calculateColorAt(ex: Float): Int {
        // Closer the indicator (handle) to the end of view the higher is hue value.
        hsvHolder[0] = floor(360f * (ex - drawingStart) / (widthF - drawingStart))

        // Brightness and saturation is left untouched.
        hsvHolder[1] = 1f
        hsvHolder[2] = 1f

        return Color.HSVToColor(hsvHolder)
    }

    override fun calculateBounds(targetWidth: Float, targetHeight: Float) {
        super.calculateBounds(targetWidth, targetHeight)
        circleColor = calculateColorAt(circleX)
    }

    override fun initializeSliderPaint() {
        hueMatrix.setTranslate(drawingStart, 0f)
        hueMatrix.postScale(
            (widthF - drawingStart) / hueBitmap.width,
            1f,
            drawingStart,
            0f
        )

        hueBitmapShader = BitmapShader(hueBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply {
            setLocalMatrix(hueMatrix)
        }

        linePaint.shader = hueBitmapShader
        isAlpha = false
    }

    fun setOnHueChangedListener(onHueChangedListener: OnHueChangedListener) {
        this.onHueChangedListener = onHueChangedListener
    }

    fun setOnHueChangedListener(onHueChangedListener: ((hue: Float, argbColor: Int) -> Unit)) {
        onHueChanged = onHueChangedListener
    }

    private fun callListeners(hue: Float, argbColor: Int) {
        onHueChanged?.invoke(hue, argbColor)
        onHueChangedListener?.onHueChanged(hue, argbColor)
    }

    interface OnHueChangedListener {
        fun onHueChanged(hue: Float, argbColor: Int)
    }
}
