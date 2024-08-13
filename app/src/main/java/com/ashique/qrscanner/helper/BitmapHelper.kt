package com.ashique.qrscanner.helper

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.core.graphics.toColorInt
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

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


    fun Context.createPatternBitmap(): Bitmap {
        val density = resources.displayMetrics.density
        val smallSquareSizeDp = 7 // Size of each square in dp
        val numColumns = 4
        val numRows = 4
        val smallSquareSizePx = (smallSquareSizeDp * density).toInt()
        val bitmapSize = smallSquareSizePx * numColumns
        val bitmap = Bitmap.createBitmap(bitmapSize, bitmapSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()

        // Draw the checkerboard pattern
        paint.color = "#50ffffff".toColorInt() // Color for the checkerboard squares
        for (row in 0 until numRows) {
            for (col in 0 until numColumns) {
                if ((row + col) % 2 == 0) {
                    canvas.drawRect(
                        (col * smallSquareSizePx).toFloat(),
                        (row * smallSquareSizePx).toFloat(),
                        ((col + 1) * smallSquareSizePx).toFloat(),
                        ((row + 1) * smallSquareSizePx).toFloat(),
                        paint
                    )
                }
            }
        }

        Log.i("ColorSlider", "createPatternBitmap: pattern bitmap created.")
        //  context.saveBitmapToFile(bitmap,"transparent")
        return bitmap
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
          // Toast.makeText(this, "QR code saved to ${file.absolutePath}", Toast.LENGTH_SHORT).show()
    }


    fun resizeBitmap(original: Bitmap, width: Int, height: Int): Bitmap {
        return Bitmap.createScaledBitmap(original, width, height, true)
    }

    fun Uri.toBitmap(context: Context): Bitmap? {
        return context.contentResolver.openInputStream(this)?.use { inputStream ->
            BitmapFactory.decodeStream(inputStream)
        }
    }

    fun Bitmap.toMutableBitmap(): Bitmap {
        return this.copy(Bitmap.Config.ARGB_8888, true)
    }

    fun Bitmap.crop(cropRect: Rect): Bitmap {
        return Bitmap.createBitmap(
            this,
            cropRect.left,
            cropRect.top,
            cropRect.width(),
            cropRect.height()
        )
    }


    fun Uri.toPath(context: Context): String? {
        // Check for "file://" scheme
        if (scheme == "file") {
            return path
        }

        // Check for "content://" scheme
        if (scheme == "content") {
            // Handle different document types
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && DocumentsContract.isDocumentUri(context, this)) {
                // DocumentProvider
                val docId = DocumentsContract.getDocumentId(this)
                val split = docId.split(":").toTypedArray()
                val type = split[0]

                if ("image" == type) {
                    val contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    val selection = "_id=?"
                    val selectionArgs = arrayOf(split[1])
                    val projection = arrayOf(MediaStore.Images.Media.DATA)

                    context.contentResolver.query(contentUri, projection, selection, selectionArgs, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val columnIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA)
                            return cursor.getString(columnIndex)
                        }
                    }
                }
            }

            // Default to querying the content resolver
            context.contentResolver.query(this, arrayOf(MediaStore.Images.Media.DATA), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val columnIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA)
                    return cursor.getString(columnIndex)
                }
            }
        }

        return null
    }

    fun Uri.toInputStream(context: Context): InputStream? {
        return try {
            context.contentResolver.openInputStream(this)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun Context.createOutputFile(extension: String): File? {
        val timestamp = System.currentTimeMillis()
        val directory = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "Qr")
        if (!directory.exists()) {
            directory.mkdirs() // Ensure the directory exists
        }
        return File(directory, "${timestamp}_binary.$extension")
    }

}