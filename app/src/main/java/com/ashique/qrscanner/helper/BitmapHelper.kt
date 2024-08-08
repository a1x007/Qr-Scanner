package com.ashique.qrscanner.helper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.media.MediaScannerConnection
import android.os.Environment
import android.util.Log
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream

object BitmapHelper {

     fun invertColors(bitmap: Bitmap): Bitmap {
        val width = 300
        val height = 300
        val resizedBitmap = resizeBitmap(bitmap, width, height)
        val grayscaleBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val pixels = IntArray(width * height)
        resizedBitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        // Convert to grayscale in a single pass
        for (i in pixels.indices) {
            val pixel = pixels[i]
            val red = Color.red(pixel)
            val green = Color.green(pixel)
            val blue = Color.blue(pixel)

            // Calculate grayscale value
            val gray = ((red * 0.299 + green * 0.587 + blue * 0.114).toInt()).coerceIn(0, 255)
            pixels[i] = Color.rgb(gray, gray, gray)
        }

        grayscaleBitmap.setPixels(pixels, 0, width, 0, 0, width, height)

        // Save the bitmap to a file(optional)
        // saveBitmapToFile(grayscaleBitmap, "result.png")
        return grayscaleBitmap
    }





    // Function to check if a bitmap is grayscale
    fun isGrayscale(bitmap: Bitmap): Boolean {
        for (x in 0 until bitmap.width) {
            for (y in 0 until bitmap.height) {
                val pixel = bitmap.getPixel(x, y)
                val red = (pixel shr 16) and 0xff
                val green = (pixel shr 8) and 0xff
                val blue = pixel and 0xff
                if (red != green || green != blue) {
                    Log.i("isGrayscale", "Pixel at ($x, $y) is not grayscale: R=$red, G=$green, B=$blue")
                    return false
                }
            }
        }
        Log.i("isGrayscale", "The bitmap is grayscale")
        return true
    }

     fun Context.saveBitmapToFile(bitmap: Bitmap, filename: String) {
        val file = File(getExternalFilesDir(null), filename)
        FileOutputStream(file).use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        Log.d("saveBitmapToFile", "Bitmap saved to: ${file.absolutePath}")
    }

    fun Context.saveBitmap(bitmap: Bitmap, format: Bitmap.CompressFormat, fileName: String) {
        val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), fileName)
        FileOutputStream(file).use { out ->
            bitmap.compress(format, 100, out)
        }

        // Notify the media scanner about the new file
        MediaScannerConnection.scanFile(this, arrayOf(file.toString()), null) { path, uri ->
            // Notify completion if needed
            Toast.makeText(this, "QR code saved to $path", Toast.LENGTH_SHORT).show()
        }
     //   Toast.makeText(this, "QR code saved to ${file.absolutePath}", Toast.LENGTH_SHORT).show()
    }


    fun resizeBitmap(original: Bitmap, width: Int, height: Int): Bitmap {
        return Bitmap.createScaledBitmap(original, width, height, true)
    }



}