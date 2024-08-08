package com.ashique.qrscanner.colorpicker

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Color
import android.graphics.ComposeShader
import android.graphics.LinearGradient
import android.graphics.PorterDuff
import android.graphics.Shader
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import com.ashique.qrscanner.R

class ColorAlphaSlider(context: Context, attributeSet: AttributeSet?) :
    ColorSlider(context, attributeSet) {

    constructor(context: Context) : this(context, null)

    private lateinit var alphaLinearGradient: LinearGradient

    var selectedColor = Color.RED
        set(value) {
            if (field != value) {
                field = value
                initializeSliderPaint()
                invalidate()
            }
        }

    private var currentAlpha = 1f

    val alphaValue: Float
        get() = currentAlpha


    private var onAlphaChanged: ((alpha: Float) -> Unit)? = null
    private var onAlphaChangedListener: OnAlphaChangedListener? = null


    override fun onCirclePositionChanged(circlePositionX: Float, circlePositionY: Float) {

        currentAlpha = calculateAlphaAt(circlePositionX)

        circleColor =
            Color.argb(
                (255 * currentAlpha).toInt(),
                Color.red(selectedColor),
                Color.green(selectedColor),
                Color.blue(selectedColor)
            )

        callListeners(currentAlpha)

        invalidate()

    }

    override fun calculateBounds(targetWidth: Float, targetHeight: Float) {
        super.calculateBounds(targetWidth, targetHeight)

        currentAlpha = calculateAlphaAt(circleX).coerceIn(0f, 1f)

        circleColor = calculateCircleColor()

    }

    private fun calculateCircleColor(): Int {
        return Color.argb(
            (255 * currentAlpha).toInt(),
            Color.red(selectedColor),
            Color.green(selectedColor),
            Color.blue(selectedColor)
        )
    }

    private fun calculateAlphaAt(ex: Float): Float {
        return (ex - drawingStart) / (widthF - drawingStart)
    }

    fun initializeSliderPaint0() {
        // Create or load your pattern bitmap
        val patternBitmap: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.transparent)

        // Calculate scale and translate to achieve center crop
        val bitmapWidth = patternBitmap.width
        val bitmapHeight = patternBitmap.height

        val scaleX = widthF / bitmapWidth
        val scaleY = heightF / bitmapHeight
        val scale = maxOf(scaleX, scaleY)

        val scaledWidth = bitmapWidth * scale
        val scaledHeight = bitmapHeight * scale

        val translateX = (widthF - scaledWidth) / 2f
        val translateY = (heightF - scaledHeight) / 2f

        // Create a new bitmap that is scaled to the required size
        val scaledBitmap = Bitmap.createScaledBitmap(patternBitmap, scaledWidth.toInt(), scaledHeight.toInt(), true)

        // Create a BitmapShader from the scaled bitmap
        val patternShader = BitmapShader(scaledBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)

        // Create your alpha gradient shader
        alphaLinearGradient = LinearGradient(
            drawingStart,
            0f,
            widthF,
            0f,
            Color.argb(
                0,
                Color.red(selectedColor),
                Color.green(selectedColor),
                Color.blue(selectedColor)
            ),
            selectedColor,
            Shader.TileMode.MIRROR
        )

        // Combine the shaders
        val composeShader = ComposeShader(alphaLinearGradient, patternShader, PorterDuff.Mode.MULTIPLY)

        // Set the shader to the paint
        linePaint.shader = composeShader

        // Initialize circle color
        circleColor = calculateCircleColor()
    }

   override fun initializeSliderPaint() {
        alphaLinearGradient =
            LinearGradient(
                drawingStart,
                0f,
                widthF,
                0f,
                Color.argb(
                    0,
                    Color.red(selectedColor),
                    Color.green(selectedColor),
                    Color.blue(selectedColor)
                ),
                selectedColor,
                Shader.TileMode.MIRROR
            )

        circleColor =
            Color.argb(
                (255 * currentAlpha).toInt(),
                Color.red(selectedColor),
                Color.green(selectedColor),
                Color.blue(selectedColor)
            )


        linePaint.shader = alphaLinearGradient
        isAlpha = true
    }

    override fun onSaveInstanceState(): Parcelable {
        return (super.onSaveInstanceState() as Bundle).apply {
            putInt(SELECTED_COLOR_KEY, selectedColor)
        }
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        selectedColor = (state as Bundle).getInt(SELECTED_COLOR_KEY)
        super.onRestoreInstanceState(state)
    }

    fun setOnAlphaChangedListener(onAlphaChangedListener: OnAlphaChangedListener) {
        this.onAlphaChangedListener = onAlphaChangedListener
    }

    fun setOnAlphaChangedListener(onAlphaChangedListener: ((alpha: Float) -> Unit)) {
        onAlphaChanged = onAlphaChangedListener
    }

    private fun callListeners(alpha: Float) {
        onAlphaChanged?.invoke(alpha)
        onAlphaChangedListener?.onAlphaChanged(alpha)
    }


    interface OnAlphaChangedListener {
        fun onAlphaChanged(hue: Float)
    }

    companion object {
        private const val SELECTED_COLOR_KEY = "sel"
    }

}