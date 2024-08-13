package com.ashique.qrscanner.helper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Movie
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.widget.ImageView
import androidx.core.graphics.drawable.toBitmap
import com.bumptech.glide.Glide
import com.bumptech.glide.gifdecoder.GifDecoder
import com.bumptech.glide.gifdecoder.GifHeader
import com.bumptech.glide.gifdecoder.GifHeaderParser
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.RequestOptions
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.ByteBuffer

object Combine {

    fun convertToBinaryBitmap(original: Bitmap, qr: Bitmap, threshold: Int = 128): Bitmap {
        // Create a new bitmap with the same dimensions as the original
        val binaryBitmap =
            Bitmap.createBitmap(original.width, original.height, Bitmap.Config.ARGB_8888)

        // Iterate over each pixel in the original bitmap
        for (y in 0 until original.height) {
            for (x in 0 until original.width) {
                // Get the pixel color from the original bitmap
                val pixelColor = original.getPixel(x, y)

                // Convert the pixel color to grayscale
                val gray =
                    (Color.red(pixelColor) * 0.299 + Color.green(pixelColor) * 0.587 + Color.blue(
                        pixelColor
                    ) * 0.114).toInt()

                // Determine if the pixel should be black or white based on the threshold
                val binaryColor = if (gray < threshold) Color.BLACK else Color.WHITE

                // Set the pixel color in the binary bitmap
                binaryBitmap.setPixel(x, y, binaryColor)
            }
        }


        return binaryBitmap //combineQrWithBinary(qr, binaryBitmap)
    }




    fun convertToDotBinaryBitmap(
        original: Bitmap,
        qr: Bitmap,
        threshold: Int = 128,
        dotSize: Int = 5
    ): Bitmap {
        // Create a new bitmap with the same dimensions as the original
        val binaryBitmap =
            Bitmap.createBitmap(original.width, original.height, Bitmap.Config.ARGB_8888)

        // Iterate over each pixel in the original bitmap
        for (y in 0 until original.height step dotSize) {
            for (x in 0 until original.width step dotSize) {
                // Calculate the average color of the area for the dot
                var totalRed = 0
                var totalGreen = 0
                var totalBlue = 0
                var pixelCount = 0

                for (dy in 0 until dotSize) {
                    for (dx in 0 until dotSize) {
                        val px = x + dx
                        val py = y + dy

                        if (px < original.width && py < original.height) {
                            val pixelColor = original.getPixel(px, py)
                            totalRed += Color.red(pixelColor)
                            totalGreen += Color.green(pixelColor)
                            totalBlue += Color.blue(pixelColor)
                            pixelCount++
                        }
                    }
                }

                // Calculate the average grayscale value
                val gray =
                    ((totalRed / pixelCount) * 0.299 + (totalGreen / pixelCount) * 0.587 + (totalBlue / pixelCount) * 0.114).toInt()

                // Determine if the dot should be black or white based on the threshold
                val binaryColor = if (gray < threshold) Color.BLACK else Color.WHITE

                // Draw the square dot on the binary bitmap
                for (dy in 0 until dotSize) {
                    for (dx in 0 until dotSize) {
                        val px = x + dx
                        val py = y + dy

                        if (px < original.width && py < original.height) {
                            binaryBitmap.setPixel(px, py, binaryColor)
                        }
                    }
                }
            }
        }

        return  blendQrWithBinaryBitmap(qr, binaryBitmap) //binaryBitmap
    }

    fun blendQrWithBinaryBitmap(qr: Bitmap, binaryBitmap: Bitmap): Bitmap {
        // Ensure binaryBitmap is resized to match QR code dimensions
        val resizedBinaryBitmap = Bitmap.createScaledBitmap(binaryBitmap, qr.width, qr.height, true)

        // Create a new bitmap for the blended result
        val blendedBitmap = Bitmap.createBitmap(qr.width, qr.height, Bitmap.Config.ARGB_8888)

        // Iterate over each pixel in the QR code bitmap
        for (y in 0 until qr.height) {
            for (x in 0 until qr.width) {
                // Get pixel color from QR code
                val qrColor = qr.getPixel(x, y)

                // Get pixel color from binary bitmap
                val binaryColor = resizedBinaryBitmap.getPixel(x, y)

                // Check if the QR code pixel is white
                if (Color.red(qrColor) == 255 && Color.green(qrColor) == 255 && Color.blue(qrColor) == 255) {
                    // If the QR code pixel is white, use the binary bitmap color
                    blendedBitmap.setPixel(x, y, binaryColor)
                } else {
                    // If the QR code pixel is black or colored, keep the QR code pixel
                    blendedBitmap.setPixel(x, y, qrColor)
                }
            }
        }

        return blendedBitmap
    }


    fun blendQrWithBinaryBitmap0(qr: Bitmap, binaryBitmap: Bitmap): Bitmap {
        // Ensure binaryBitmap is resized to match QR code dimensions
        val resizedBinaryBitmap = Bitmap.createScaledBitmap(binaryBitmap, qr.width, qr.height, true)

        // Create a new bitmap for the blended result
        val blendedBitmap = Bitmap.createBitmap(qr.width, qr.height, Bitmap.Config.ARGB_8888)

        // Iterate over each pixel in the QR code bitmap
        for (y in 0 until qr.height) {
            for (x in 0 until qr.width) {
                // Get pixel color from QR code
                val qrColor = qr.getPixel(x, y)

                // Get pixel color from binary bitmap
                val binaryColor = resizedBinaryBitmap.getPixel(x, y)

                // Blend the QR code pixel with the binary bitmap pixel
                // Here, we're simply choosing the QR code pixel if it's not black; otherwise, we use the binary bitmap color
                val blendedColor = if (Color.red(qrColor) == 0 && Color.green(qrColor) == 0 && Color.blue(qrColor) == 0) {
                    binaryColor
                } else {
                    qrColor
                }

                // Set the pixel color in the blended bitmap
                blendedBitmap.setPixel(x, y, blendedColor)
            }
        }

        return blendedBitmap
    }


    fun generateQrCodeWithBinaryBitmap(data: String, original: Bitmap, threshold: Int = 128, dotSize: Int = 5): Bitmap {
        // Convert the original bitmap to a dot binary bitmap
        val binaryBitmap = convertToDotBinaryBitmap(original, qr = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888), threshold, dotSize)

        // Generate the QR code bitmap
        val qrCodeWriter = QRCodeWriter()
        val qrCodeBitMatrix = qrCodeWriter.encode(data, BarcodeFormat.QR_CODE, 200, 200) // Adjust the size as needed
        val qrCodeBitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)

        for (x in 0 until qrCodeBitmap.width) {
            for (y in 0 until qrCodeBitmap.height) {
                qrCodeBitmap.setPixel(x, y, if (qrCodeBitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
            }
        }

        // Combine the binary bitmap with the QR code
        val combinedBitmap = Bitmap.createBitmap(qrCodeBitmap.width, qrCodeBitmap.height, Bitmap.Config.ARGB_8888)

        val canvas = Canvas(combinedBitmap)
        canvas.drawBitmap(qrCodeBitmap, 0f, 0f, null)
        canvas.drawBitmap(binaryBitmap, 0f, 0f, null) // Overlay the binary bitmap on top of the QR code

        return combinedBitmap
    }



}