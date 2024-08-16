package com.ashique.qrscanner.helper

import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import com.ashique.qrscanner.helper.ImageConverter.toBinaryBitmap
import com.waynejo.androidndkgif.GifDecoder
import com.waynejo.androidndkgif.GifEncoder
import java.io.File
import java.io.FileNotFoundException
import java.util.LinkedList

class GifPipeline {
    var outputFile: File? = null
    var clippingRect: RectF? = null
    var errorInfo: String? = null

    private var gifDecoder: GifDecoder? = null
    private var frameSequence = LinkedList<Bitmap>()
    private var currentFrame = 0

    private val TAG = "GifPipeline"

    fun init(file: File): Boolean {
        if (!file.exists()) {
            errorInfo = "ENOENT: File does not exist."
            Log.e(TAG, errorInfo!!)
            return false
        } else if (file.isDirectory) {
            errorInfo = "EISDIR: Target is a directory."
            Log.e(TAG, errorInfo!!)
            return false
        }
        gifDecoder = GifDecoder()
        val isSucceeded = gifDecoder!!.load(file.absolutePath)
        if (!isSucceeded) {
            errorInfo = "Failed to decode input file as GIF."
            Log.e(TAG, errorInfo!!)
            return false
        }
        return true
    }

    fun nextFrame(crop: Boolean? = false, convertBinary: Boolean? = false): Bitmap? {
        if (gifDecoder!!.frameNum() == 0) {
            errorInfo = "GIF contains zero frames."
            Log.e(TAG, errorInfo!!)
            return null
        }

        if (currentFrame < gifDecoder!!.frameNum()) {
            val frame = gifDecoder!!.frame(currentFrame)
            currentFrame++

            // If cropping is enabled and a clippingRect is provided
            if (crop == true && clippingRect != null) {
                // Adjust clippingRect if it's out of bounds
                val adjustedRect = RectF(
                    clippingRect!!.left,
                    clippingRect!!.top,
                    clippingRect!!.right.coerceAtMost(frame.width.toFloat()),
                    clippingRect!!.bottom.coerceAtMost(frame.height.toFloat())
                )

                if (adjustedRect.width() <= 0 || adjustedRect.height() <= 0) {
                    errorInfo = "Adjusted clipping rect is invalid."
                    Log.e(
                        TAG,
                        "$errorInfo AdjustedRect: $adjustedRect, Frame size: ${frame.width}x${frame.height}"
                    )
                    frame.recycle()
                    return null
                }

                val cropped = Bitmap.createBitmap(
                    frame,
                    Math.round(adjustedRect.left),
                    Math.round(adjustedRect.top),
                    Math.round(adjustedRect.width()),
                    Math.round(adjustedRect.height())
                )
                frame.recycle()
                return cropped
            } else {
                // If cropping is not enabled, return the entire frame
                return if (convertBinary == true) {
                    toBinaryBitmap(
                        frame, colorize = false, shapeSize = 3, threshold = 127, useShape = false
                    )

                } else frame
            }
        } else {
            errorInfo = "No more frames available."
            Log.e(TAG, errorInfo!!)
            return null
        }
    }


    fun pushRendered(bitmap: Bitmap) {
        frameSequence.addLast(bitmap)
    }

    fun postRender(): Boolean {
        if (outputFile == null) {
            errorInfo = "Output file is not yet set."
            Log.e(TAG, errorInfo!!)
            return false
        }

        if (frameSequence.size == 0) {
            errorInfo = "Zero frames in the sequence."
            Log.e(TAG, errorInfo!!)
            return false
        }

        try {
            val gifEncoder = GifEncoder()
            gifEncoder.init(
                frameSequence.first.width,
                frameSequence.first.height,
                outputFile!!.absolutePath,
                GifEncoder.EncodingType.ENCODING_TYPE_FAST
            )
            val frameIndex = 0
            while (!frameSequence.isEmpty()) {
                gifEncoder.encodeFrame(frameSequence.removeFirst(), gifDecoder!!.delay(frameIndex))
            }
            gifEncoder.close()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            errorInfo = "FileNotFoundException. See stacktrace for more information."
            Log.e(TAG, errorInfo!!)
            return false
        }

        return true
    }

    fun release(): Boolean {
        return true
    }
}