package com.ashique.qrscanner.helper

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint


object ImageConverter {

    fun convertImageToHalftone(bitmap: Bitmap, colorized: Boolean, dotSize: Int = 10): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val halftoneBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(halftoneBitmap)
        val paint = Paint()


        if (colorized) {
            // Apply colorized halftone effect
            for (y in 0 until height step dotSize) {
                for (x in 0 until width step dotSize) {
                    val color = bitmap.getPixel(x, y)
                    val gray = (Color.red(color) + Color.green(color) + Color.blue(color)) / 3
                    val radius = dotSize * (1 - gray / 255.0).toFloat()
                    paint.color = color
                    paint.style = Paint.Style.FILL
                    canvas.drawCircle(
                        x.toFloat() + dotSize / 2,
                        y.toFloat() + dotSize / 2,
                        radius,
                        paint
                    )
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
                    canvas.drawCircle(
                        x.toFloat() + dotSize / 2,
                        y.toFloat() + dotSize / 2,
                        radius,
                        paint
                    )
                }
            }
        }

        return halftoneBitmap
    }

    fun toBinaryBitmap(
        inputBitmap: Bitmap,
        threshold: Int = 128,
        shapeSize: Int = 10,
        colorize: Boolean = false,
        useShape: Boolean = true
    ): Bitmap {
        val width = inputBitmap.width
        val height = inputBitmap.height
        val outputBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(outputBitmap)
        val paint = Paint()

        val colorArray = Array(height) { IntArray(width) }

        // Prepare pixel colors
        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = inputBitmap.getPixel(x, y)
                colorArray[y][x] = if (colorize) pixel else {
                    val gray = (Color.red(pixel) * 0.3 + Color.green(pixel) * 0.59 + Color.blue(pixel) * 0.11).toInt()
                    Color.rgb(gray, gray, gray)
                }
            }
        }

        // Apply Atkinson dithering and drawing
        for (y in 0 until height step shapeSize) {
            for (x in 0 until width step shapeSize) {
                val oldPixel = colorArray[y][x]
                val newPixel = if (colorize) {
                    val red = if (Color.red(oldPixel) > threshold) 255 else 0
                    val green = if (Color.green(oldPixel) > threshold) 255 else 0
                    val blue = if (Color.blue(oldPixel) > threshold) 255 else 0
                    Color.rgb(red, green, blue)
                } else {
                    if (Color.red(oldPixel) > threshold) Color.WHITE else Color.BLACK
                }

                // Draw the custom shape or fill the area
                if (useShape) {
                    paint.color = newPixel
                    canvas.drawCircle((x + shapeSize / 2).toFloat(), (y + shapeSize / 2).toFloat(), (shapeSize / 2).toFloat(), paint)
                } else {
                    for (dy in 0 until shapeSize) {
                        for (dx in 0 until shapeSize) {
                            if (y + dy < height && x + dx < width) {
                                outputBitmap.setPixel(x + dx, y + dy, newPixel)
                            }
                        }
                    }
                }

                // Spread quantization error
                spreadError(colorArray, x, y, oldPixel, newPixel, width, height, shapeSize)
            }
        }

        return outputBitmap
    }

    private fun spreadError(
        colorArray: Array<IntArray>,
        x: Int,
        y: Int,
        oldPixel: Int,
        newPixel: Int,
        width: Int,
        height: Int,
        shapeSize: Int
    ) {
        val redError = Color.red(oldPixel) - Color.red(newPixel)
        val greenError = Color.green(oldPixel) - Color.green(newPixel)
        val blueError = Color.blue(oldPixel) - Color.blue(newPixel)

        fun distributeError(x: Int, y: Int) {
            if (x in 0 until width && y in 0 until height) {
                val oldColor = colorArray[y][x]
                val newRed = (Color.red(oldColor) + redError * 1 / 8).coerceIn(0, 255)
                val newGreen = (Color.green(oldColor) + greenError * 1 / 8).coerceIn(0, 255)
                val newBlue = (Color.blue(oldColor) + blueError * 1 / 8).coerceIn(0, 255)
                colorArray[y][x] = Color.rgb(newRed, newGreen, newBlue)
            }
        }

        distributeError(x + shapeSize, y)
        distributeError(x + 2 * shapeSize, y)
        distributeError(x - shapeSize, y + shapeSize)
        distributeError(x, y + shapeSize)
        distributeError(x + shapeSize, y + shapeSize)
        distributeError(x, y + 2 * shapeSize)
    }





    fun toBinaryBitmap(inputBitmap: Bitmap, threshold: Int = 128, pixelSize: Int = 3): Bitmap {
        val width = inputBitmap.width
        val height = inputBitmap.height
        val outputBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        // Convert image to grayscale first
        val grayscale = Array(height) { IntArray(width) }

        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = inputBitmap.getPixel(x, y)
                val gray =
                    (Color.red(pixel) * 0.3 + Color.green(pixel) * 0.59 + Color.blue(pixel) * 0.11).toInt()
                grayscale[y][x] = gray
            }
        }

        // Apply Atkinson dithering with larger pixel blocks
        for (y in 0 until height step pixelSize) {
            for (x in 0 until width step pixelSize) {
                val oldPixel = grayscale[y][x]
                val newPixel = if (oldPixel > threshold) 255 else 0 // Binary threshold
                val quantError = oldPixel - newPixel

                // Set the entire block to the same color
                for (dy in 0 until pixelSize) {
                    for (dx in 0 until pixelSize) {
                        if (y + dy < height && x + dx < width) {
                            outputBitmap.setPixel(
                                x + dx,
                                y + dy,
                                if (newPixel == 255) Color.WHITE else Color.BLACK
                            )
                        }
                    }
                }

                // Spread the quantization error to neighboring pixels
                if (x + pixelSize < width) grayscale[y][x + pixelSize] += quantError * 1 / 8
                if (x + 2 * pixelSize < width) grayscale[y][x + 2 * pixelSize] += quantError * 1 / 8
                if (y + pixelSize < height) {
                    if (x > 0) grayscale[y + pixelSize][x - pixelSize] += quantError * 1 / 8
                    grayscale[y + pixelSize][x] += quantError * 1 / 8
                    if (x + pixelSize < width) grayscale[y + pixelSize][x + pixelSize] += quantError * 1 / 8
                }
                if (y + 2 * pixelSize < height) grayscale[y + 2 * pixelSize][x] += quantError * 1 / 8
            }
        }

        return outputBitmap
    }


    fun convertToBinaryBitmap0(inputBitmap: Bitmap, threshold: Int = 128): Bitmap {
        val width = inputBitmap.width
        val height = inputBitmap.height
        val outputBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        // Convert image to grayscale first
        val grayscale = Array(height) { IntArray(width) }

        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = inputBitmap.getPixel(x, y)
                val gray =
                    (Color.red(pixel) * 0.3 + Color.green(pixel) * 0.59 + Color.blue(pixel) * 0.11).toInt()
                grayscale[y][x] = gray
            }
        }

        // Apply Atkinson dithering
        for (y in 0 until height) {
            for (x in 0 until width) {
                val oldPixel = grayscale[y][x]
                val newPixel = if (oldPixel > 128) 255 else 0 // Binary threshold
                val quantError = oldPixel - newPixel

                outputBitmap.setPixel(x, y, if (newPixel == 255) Color.WHITE else Color.BLACK)

                // Spread the quantization error to neighboring pixels
                if (x + 1 < width) grayscale[y][x + 1] += quantError * 1 / 8
                if (x + 2 < width) grayscale[y][x + 2] += quantError * 1 / 8
                if (y + 1 < height) {
                    if (x > 0) grayscale[y + 1][x - 1] += quantError * 1 / 8
                    grayscale[y + 1][x] += quantError * 1 / 8
                    if (x + 1 < width) grayscale[y + 1][x + 1] += quantError * 1 / 8
                }
                if (y + 2 < height) grayscale[y + 2][x] += quantError * 1 / 8
            }
        }

        return outputBitmap
    }

    fun convertBitmap(bitmap: Bitmap): Bitmap? {
        val width = bitmap.width
        val height = bitmap.height
        val processedBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val errors = Array(width) { IntArray(height) }

        // Iterate over the image pixels
        for (y in 0 until height) {
            for (x in 0 until width) {
                // Get the current pixel
                val oldPixel = Color.red(processedBitmap.getPixel(x, y)) + errors[x][y]

                // Determine the new pixel value (1-bit color)
                val newPixel = if (oldPixel > 127) 255 else 0
                val quantError = oldPixel - newPixel

                // Set the new pixel to black or white
                val color = if (newPixel == 255) Color.WHITE else Color.BLACK
                processedBitmap.setPixel(x, y, color)

                // Distribute the quantization error
                if (x + 1 < width) errors[x + 1][y] += quantError * 1 / 8
                if (x + 2 < width) errors[x + 2][y] += quantError * 1 / 8
                if (y + 1 < height) {
                    if (x - 1 >= 0) errors[x - 1][y + 1] += quantError * 1 / 8
                    errors[x][y + 1] += quantError * 1 / 8
                    if (x + 1 < width) errors[x + 1][y + 1] += quantError * 1 / 8
                }
                if (y + 2 < height) errors[x][y + 2] += quantError * 1 / 8
            }
        }
        return processedBitmap
    }



}