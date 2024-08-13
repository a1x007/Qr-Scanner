package com.ashique.qrscanner.helper

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.Log
import com.ashique.qrscanner.helper.Combine.convertToDotBinaryBitmap
import com.ashique.qrscanner.helper.ImageConverter.convertImageToDotArt
import com.ashique.qrscanner.helper.ImageConverter.convertImageToHalftone
import com.waynejo.androidndkgif.GifDecoder
import com.waynejo.androidndkgif.GifEncoder
import java.io.File
import java.io.FileNotFoundException
import java.util.LinkedList
import kotlin.math.roundToInt

class GifPipeline2 {
    var outputFile: File? = null
    var clippingRect: RectF? = null
    var errorInfo: String? = null
    private var gifDecoder: GifDecoder? = null
    private var frameSequence = LinkedList<Bitmap>()
    private var currentFrame = 0
    var qrBitmap: Bitmap? = null
    var useBinary = true
    val TAG = "GifPipeline"

    fun init(file: File): Boolean {
        if (!file.exists()) {
            errorInfo = "ENOENT: File does not exist."
            Log.e(TAG, "init: ENOENT: File does not exist.")
            return false
        } else if (file.isDirectory) {
            errorInfo = "EISDIR: Target is a directory."
            Log.e(TAG, "init: EISDIR: Target is a directory.")
            return false
        }
        gifDecoder = GifDecoder()
        val isSucceeded = gifDecoder!!.load(file.absolutePath)
        if (!isSucceeded) {
            errorInfo = "Failed to decode input file as GIF."
            Log.e(TAG, "init: Failed to decode input file as GIF.")
            return false
        }
        return true
    }

    fun nextFrame(): Bitmap? {
        if (gifDecoder!!.frameNum() == 0) {
            errorInfo = "GIF contains zero frames."
            Log.e(TAG, "nextFrame: GIF contains zero frames.")
            return null
        }
        if (currentFrame < gifDecoder!!.frameNum()) {
            val frame = gifDecoder!!.frame(currentFrame)
            currentFrame++

            // Directly pass the full GIF frame to blendQrBitmap
            val blendedFrame = if (useBinary) {
                convertImageToHalftone(frame, false)
               // processBinary(frame)
                //convertGifToBinary(frame, qrBitmap)
            } else {
                blendQrBitmap(frame, qrBitmap)
            }

            return blendedFrame
        } else {
            return null
        }
    }

    fun nextFrame0(): Bitmap? {
        if (gifDecoder!!.frameNum() == 0) {
            errorInfo = "GIF contains zero frames."
            Log.e(TAG, "nextFrame: GIF contains zero frames.")
            return null
        }
        if (clippingRect == null) {
            errorInfo = "No cropping rect provided."
            Log.e(TAG, "nextFrame: No cropping rect provided.")
            return null
        }
        if (currentFrame < gifDecoder!!.frameNum()) {
            val frame = gifDecoder!!.frame(currentFrame)
            currentFrame++

            val left = clippingRect!!.left.roundToInt().coerceIn(0, frame.width)
            val top = clippingRect!!.top.roundToInt().coerceIn(0, frame.height)
            val right = clippingRect!!.right.roundToInt().coerceIn(left, frame.width)
            val bottom = clippingRect!!.bottom.roundToInt().coerceIn(top, frame.height)

            val cropped = Bitmap.createBitmap(
                frame,
                left,
                top,
                right - left,
                bottom - top
            )

            // Resize the cropped frame to match the size of the QR bitmap
            val qrWidth = qrBitmap?.width ?: cropped.width
            val qrHeight = qrBitmap?.height ?: cropped.height
            val resizedFrame = Bitmap.createScaledBitmap(cropped, qrWidth, qrHeight, true)

            // Blend the QR bitmap onto the resized GIF frame
            val blendedFrame = if (useBinary) {
                convertGifToBinary(resizedFrame, qrBitmap)
            } else {
                blendQrBitmap(resizedFrame, qrBitmap)
            }

            // Recycle the resized frame if it's no longer needed
            resizedFrame.recycle()

            return blendedFrame
        } else {
            return null
        }
    }




    fun convertGifToBinary(frameBitmap: Bitmap, qrBitmap: Bitmap?, threshold: Int = 128, dotSize: Int = 5): Bitmap? {
        // Convert the frame to a binary bitmap
        val binaryFrame =
            qrBitmap?.let { convertToDotBinaryBitmap(frameBitmap, it, threshold, dotSize) }

        // Blend the QR bitmap onto the binary frame
        return binaryFrame?.let { blendQrBitmap(it, qrBitmap) }
    }

    private fun processBinary(original: Bitmap): Bitmap {
        val dotSize = 4
        // Convert the original image to grayscale
        val grayBitmap = convertToGrayscale(original)

        // Create a new bitmap for the dot-like binary image
        val width = grayBitmap.width
        val height = grayBitmap.height
        val dotBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(dotBitmap)
        val paint = Paint()

        // Calculate the threshold for binary conversion
        val threshold = 128 // Adjust as necessary

        for (y in 0 until height step dotSize) {
            for (x in 0 until width step dotSize) {
                // Get the average color of the dot area
                var totalColor = 0
                var count = 0

                for (dy in 0 until dotSize) {
                    for (dx in 0 until dotSize) {
                        if (x + dx < width && y + dy < height) {
                            totalColor += grayBitmap.getPixel(x + dx, y + dy) and 0xFF
                            count++
                        }
                    }
                }

                // Compute the average color and decide if it's black or white
                val averageColor = totalColor / count
                val color = if (averageColor > threshold) Color.WHITE else Color.BLACK

                // Draw the dot
                paint.color = color
                canvas.drawCircle((x + dotSize / 2).toFloat(), (y + dotSize / 2).toFloat(), (dotSize / 2).toFloat(), paint)
            }
        }

        return dotBitmap
    }

    fun convertToGrayscale(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val grayBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = bitmap.getPixel(x, y)
                val red = Color.red(pixel)
                val green = Color.green(pixel)
                val blue = Color.blue(pixel)
                val gray = (0.299 * red + 0.587 * green + 0.114 * blue).toInt()
                val newPixel = Color.rgb(gray, gray, gray)
                grayBitmap.setPixel(x, y, newPixel)
            }
        }

        return grayBitmap
    }


    // not using
    private fun blendQrBitmap0(frame: Bitmap, qrBitmap: Bitmap?): Bitmap {
        if (qrBitmap == null) return frame

        // Create a mutable bitmap to hold the blended result, using the frame's configuration
        val result = Bitmap.createBitmap(frame.width, frame.height, frame.config!!)

        // Create a canvas to draw on the result bitmap
        val canvas = Canvas(result)

        // Draw the GIF frame onto the result bitmap
        canvas.drawBitmap(frame, 0f, 0f, null)

        // Calculate the size and position of the QR bitmap
        val qrWidth = qrBitmap.width
        val qrHeight = qrBitmap.height
        val scaleFactor = minOf(frame.width.toFloat() / qrWidth, frame.height.toFloat() / qrHeight)
        val scaledQrWidth = (qrWidth * scaleFactor).roundToInt()
        val scaledQrHeight = (qrHeight * scaleFactor).roundToInt()

        // Create a scaled version of the QR bitmap
        val scaledQrBitmap = Bitmap.createScaledBitmap(qrBitmap, scaledQrWidth, scaledQrHeight, true)

        // Calculate the position to center the scaled QR bitmap on the frame
        val left = (frame.width - scaledQrWidth) / 2
        val top = (frame.height - scaledQrHeight) / 2

        // Draw the scaled QR bitmap on top of the GIF frame
        canvas.drawBitmap(scaledQrBitmap, left.toFloat(), top.toFloat(), null)

        // Recycle the scaled QR bitmap if it's no longer needed
        scaledQrBitmap.recycle()

        return result
    }


    private fun blendQrBitmap(frame: Bitmap, qrBitmap: Bitmap?): Bitmap {
        if (qrBitmap == null) return frame

        // Resize the GIF frame to match the size of the QR bitmap
        val resizedFrame = Bitmap.createScaledBitmap(frame, qrBitmap.width, qrBitmap.height, true)

        // Create a mutable bitmap to hold the blended result, using the resized frame's configuration
        val result = Bitmap.createBitmap(resizedFrame.width, resizedFrame.height, resizedFrame.config!!)

        // Create a canvas to draw on the result bitmap
        val canvas = Canvas(result)

        // Draw the resized GIF frame onto the result bitmap
        canvas.drawBitmap(resizedFrame, 0f, 0f, null)

        // Calculate the size and position of the QR bitmap
        val qrWidth = qrBitmap.width
        val qrHeight = qrBitmap.height

        // Calculate the position to center the QR bitmap on the resized frame
        val left = (resizedFrame.width - qrWidth) / 2
        val top = (resizedFrame.height - qrHeight) / 2

        // Draw the QR bitmap on top of the resized GIF frame
        canvas.drawBitmap(qrBitmap, left.toFloat(), top.toFloat(), null)

        // Recycle the resized frame if it's no longer needed
        resizedFrame.recycle()

        return result
    }


    fun pushRendered(bitmap: Bitmap) {
        frameSequence.addLast(bitmap)
        Log.i(TAG, "pushRendered: bitmap: $bitmap ")
    }

    fun postRender(): Boolean {
        if (outputFile == null) {
            errorInfo = "Output file is not yet set."
            Log.e(TAG, "postRender: Output file is not yet set")
            return false
        }

        if (frameSequence.isEmpty()) {
            errorInfo = "Zero frames in the sequence."
            Log.e(TAG, "postRender: Zero frames in the sequence")
            return false
        }

        try {
            val gifEncoder = GifEncoder()
            gifEncoder.init(frameSequence.first.width, frameSequence.first.height, outputFile!!.absolutePath, GifEncoder.EncodingType.ENCODING_TYPE_FAST)
            var frameIndex = 0
            while (frameSequence.isNotEmpty()) {
                gifEncoder.encodeFrame(frameSequence.removeFirst(), gifDecoder!!.delay(frameIndex))
                frameIndex++
            }
            gifEncoder.close()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            errorInfo = "FileNotFoundException. See stacktrace for more information."
            Log.e(TAG, "postRender: FileNotFoundException. See stacktrace for more information.")
            return false
        } catch (e: Exception) {
            e.printStackTrace()
            errorInfo = "Exception during GIF encoding: ${e.message}"
            Log.e(TAG, "postRender: Exception during GIF encoding: ${e.message}")
            return false
        }

        return true
    }

    fun release(): Boolean {
        return true
    }

}
