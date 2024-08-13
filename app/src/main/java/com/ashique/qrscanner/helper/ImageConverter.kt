package com.ashique.qrscanner.helper

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

object ImageConverter {

    fun convertImageToHalftone(bitmap: Bitmap, colorized: Boolean): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val halftoneBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(halftoneBitmap)
        val paint = Paint()

        // Define dot size
        val dotSize = 10 // Size of the halftone dots

        if (colorized) {
            // Apply colorized halftone effect
            for (y in 0 until height step dotSize) {
                for (x in 0 until width step dotSize) {
                    val color = bitmap.getPixel(x, y)
                    val gray = (Color.red(color) + Color.green(color) + Color.blue(color)) / 3
                    val radius = dotSize * (1 - gray / 255.0).toFloat()
                    paint.color = color
                    paint.style = Paint.Style.FILL
                    canvas.drawCircle(x.toFloat() + dotSize / 2, y.toFloat() + dotSize / 2, radius, paint)
                }
            }
        } else {
            // Apply grayscale halftone effect
            val grayscaleBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            for (y in 0 until height) {
                for (x in 0 until width) {
                    val color = bitmap.getPixel(x, y)
                    val red = Color.red(color)
                    val green = Color.green(color)
                    val blue = Color.blue(color)
                    val gray = (red + green + blue) / 3
                    grayscaleBitmap.setPixel(x, y, Color.rgb(gray, gray, gray))
                }
            }

            for (y in 0 until height step dotSize) {
                for (x in 0 until width step dotSize) {
                    val gray = Color.red(grayscaleBitmap.getPixel(x, y)) // Assuming grayscale
                    val radius = dotSize * (1 - gray / 255.0).toFloat()
                    paint.color = Color.BLACK
                    paint.style = Paint.Style.FILL
                    canvas.drawCircle(x.toFloat() + dotSize / 2, y.toFloat() + dotSize / 2, radius, paint)
                }
            }
        }

        return halftoneBitmap
    }


    fun convertImageToDotArt(bitmap: Bitmap): Bitmap {
        val dotSize = 2
        val padding = 2
        val width = bitmap.width
        val height = bitmap.height
        val dotArtBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(dotArtBitmap)
        val paint = Paint()

        for (y in 0 until height step (dotSize + padding)) {
            for (x in 0 until width step (dotSize + padding)) {
                // Get the color of the pixel at (x, y) and convert to grayscale
                val color = bitmap.getPixel(x, y)
                val grayColor = toGrayscale(color)



                paint.color = grayColor


                // Calculate the position and size for the square, considering padding
                val left = x.toFloat() + padding
                val top = y.toFloat() + padding
                val right = left + dotSize
                val bottom = top + dotSize

                canvas.drawRect(left, top, right, bottom, paint)
            }
        }

        return dotArtBitmap
    }

    fun toGrayscale(color: Int): Int {
        val red = Color.red(color)
        val green = Color.green(color)
        val blue = Color.blue(color)
        // Calculate grayscale value using the luminance formula
        val gray = (0.299 * red + 0.587 * green + 0.114 * blue).toInt()
        return Color.rgb(gray, gray, gray)
    }


}