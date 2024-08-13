package com.ashique.qrscanner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Bitmap.Config
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.AsyncTask
import android.util.AttributeSet
import kotlin.math.round

class AsciiImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : androidx.appcompat.widget.AppCompatImageView(context, attrs, defStyleAttr) {

    private val chars = arrayOf("@", "#", "+", "\\", ";", ":", ",", ".", "`", " ")
    private var color: Boolean = false
    private var quality: Int = 13
    private var qualityColor: Int = 6

    init {
        attrs?.let {
            val a = context.obtainStyledAttributes(it, R.styleable.AsciiImageView, 0, 0)
            color = a.getBoolean(R.styleable.AsciiImageView__color, false)
            quality = a.getDimension(R.styleable.AsciiImageView__quality, 3f).toInt()
            a.recycle()
        }
        updateImage()
    }

    fun setColor(color: Boolean) {
        this.color = color
        updateImage()
    }

    fun setQuality(quality: Int) {
        this.quality = quality
        updateImage()
    }

    private fun updateImage() {
        val drawable = drawable ?: return
        val bitmap = drawableToBitmap(drawable) ?: return
        AsciiConverterTask(bitmap, color, quality, qualityColor, chars, ::onConversionComplete).execute()
    }

    private fun onConversionComplete(result: Bitmap) {
        setImageBitmap(result)
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap? {
        return (drawable as? BitmapDrawable)?.bitmap
            ?: BitmapFactory.decodeResource(resources, R.drawable.goku)
    }

    private class AsciiConverterTask(
        private val rgbImage: Bitmap,
        private val color: Boolean,
        private var quality: Int,
        private val qualityColor: Int,
        private val chars: Array<String>,
        private val callback: (Bitmap) -> Unit
    ) : AsyncTask<Void, Int, Bitmap>() {

        override fun doInBackground(vararg params: Void?): Bitmap {
            if (color) {
                quality += qualityColor
                if (quality > 5 + qualityColor || quality < 1 + qualityColor) {
                    quality = 3 + qualityColor
                }
            } else {
                if (quality > 5 || quality < 1) {
                    quality = 3
                }
            }

            val width = rgbImage.width
            val height = rgbImage.height
            val paint = Paint()
            val textPaint = Paint().apply {
                color = Color.BLACK
                textSize = (quality * 2).toFloat() // Adjust text size based on quality
                textAlign = Paint.Align.LEFT
                isAntiAlias = true
            }

            val bitmap = Bitmap.createBitmap(width, height, Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            val scaleX = width.toFloat() / (width / quality)
            val scaleY = height.toFloat() / (height / quality)

            for (y in 0 until height step quality) {
                for (x in 0 until width step quality) {
                    val pixel = rgbImage.getPixel(x, y)
                    val red = Color.red(pixel)
                    val green = Color.green(pixel)
                    val blue = Color.blue(pixel)
                    val tx: String
                    val scaledX = (x / quality * quality).toFloat()
                    val scaledY = (y / quality * quality).toFloat()

                    if (color) {
                        tx = "."
                        paint.color = Color.rgb(red, green, blue)
                        canvas.drawText(tx, scaledX, scaledY + textPaint.textSize, paint)
                    } else {
                        val brightness = (red + green + blue) / 3
                        val roundedBrightness = round((brightness / (255 / (chars.size - 1))).toDouble()).toInt()
                        tx = chars[roundedBrightness]
                        canvas.drawText(tx, scaledX, scaledY + textPaint.textSize, textPaint)
                    }
                }
                publishProgress(y, height)
            }
            return bitmap
        }

        override fun onPostExecute(result: Bitmap) {
            super.onPostExecute(result)
            callback(result)
        }
    }

}
