package com.ashique.qrscanner.activity

import android.R.attr
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.widget.addTextChangedListener
import com.ashique.qrscanner.R
import com.ashique.qrscanner.databinding.ActivityGeneratorBinding
import com.ashique.qrscanner.helper.BitmapHelper.crop
import com.ashique.qrscanner.helper.BitmapHelper.saveBitmap
import com.ashique.qrscanner.helper.BitmapHelper.toBitmap
import com.ashique.qrscanner.helper.BitmapHelper.toMutableBitmap
import com.ashique.qrscanner.helper.Extensions.showToast
import com.ashique.qrscanner.helper.Extensions.toBitmapDrawable
import com.ashique.qrscanner.helper.QrHelper
import com.github.sumimakito.awesomeqr.AwesomeQrRenderer
import com.github.sumimakito.awesomeqr.RenderResult
import com.github.sumimakito.awesomeqr.option.RenderOption
import com.github.sumimakito.awesomeqr.option.background.Background
import com.github.sumimakito.awesomeqr.option.background.BlendBackground
import com.github.sumimakito.awesomeqr.option.background.GifBackground
import com.github.sumimakito.awesomeqr.option.background.StillBackground
import com.github.sumimakito.awesomeqr.option.color.Color
import com.github.sumimakito.awesomeqr.option.logo.Logo
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.io.File


class GeneratorActivity : AppCompatActivity() {

    private lateinit var ui: ActivityGeneratorBinding
    val rainbowColor = Color().apply {
        light = 0xFFFFFFFF.toInt() // for blank spaces
        dark = 0xFFFF8C8C.toInt() // for non-blank spaces
        background =
            0xFFFFFFFF.toInt() // for the background (will be overriden by background images, if set)
        auto =
            false // set to true to automatically pick out colors from the background image (will only work if background image is present)
    }

    private var backgroundType = "still"
    private var isLogo = false
    private var useRainbow = true
    private var logoBitmap: Bitmap? = null
    private var backgroundBitmap: Bitmap? = null
    private var gifFile: File? = null
    private var pictureStorage =
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)

    private lateinit var photoPickerLauncher: ActivityResultLauncher<String>
    val TAG = "Generator"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = ActivityGeneratorBinding.inflate(layoutInflater)
        setContentView(ui.root)

        generateQr()

        photoPickerLauncher = registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->
            uri?.toBitmap(this)?.let { bitmap ->
                if (isLogo) {
                    logoBitmap = bitmap.toMutableBitmap()
                } else {
                    backgroundBitmap = bitmap.toMutableBitmap()
                }
                renderQr()

            }

        }

        ui.editInput.setText("Special,thus awesome.")
        ui.editInput.addTextChangedListener { generateQr() }
        ui.generateBtn.setOnClickListener { generateQr() }
        ui.uploadLogoBtn.setOnClickListener { importDrawable(true) }
        ui.uploadStillBgBtn.setOnClickListener { importDrawable(false) }
        ui.uploadBlendBgBtn.setOnClickListener { importDrawable(false, type = "blend") }
        ui.uploadGifBgBtn.setOnClickListener { importDrawable(false, type = "gif") }


        ui.saveBtn.setOnClickListener {
            ui.qrPreview.drawable?.let { drawable ->
                val bitmap = drawable.toBitmap(1024, 1024)
                if (QrHelper.scanQrCode(bitmap).isNullOrEmpty()) {
                    ui.verifyQrText.apply {
                        setTextColor(android.graphics.Color.RED)
                        text = String.format("Qr is corrupted !")
                    }

                } else {
                    ui.verifyQrText.apply {
                        setTextColor(android.graphics.Color.GREEN)
                        text = String.format("Qr is verified.")
                    }

                }
                saveBitmap(bitmap, Bitmap.CompressFormat.JPEG, "qr_code.png")
            } ?: run {
                Toast.makeText(
                    this, "QR code image is not available.", Toast.LENGTH_SHORT
                ).show()
            }
        }
    }


    private fun generateQr() {
        // Render the QR code asynchronously
        AwesomeQrRenderer.renderAsync(renderQr(), { result ->
            Log.i(TAG, "renderAsync Qr: $result")
            runOnUiThread {
                when {
                    result.bitmap != null -> {
                        // If the result has a bitmap, display it
                        ui.qrPreview.setImageBitmap(result.bitmap)
                    }

                    result.type == RenderResult.OutputType.GIF -> {
                        // If the background is a GIF, the image will be saved to the output file
                        showToast("GIF saved at: ${pictureStorage?.absolutePath}")

                        Log.i(TAG, "GIF saved at: ${pictureStorage?.absolutePath}")
                    }

                    else -> {
                        // Handle unexpected results
                        showToast("Unexpected result")
                    }
                }
            }
        }, { exception ->
            Log.e(TAG, "Error generating QR code: $exception")
            runOnUiThread {
                // Handle exceptions during rendering
                exception.printStackTrace()
                showToast("Error generating QR code: ${exception.message}")
            }
        })
    }

    private fun renderQr(): RenderOption {

        Log.d(TAG, "Logo Bitmap: $logoBitmap")
        Log.d(TAG, "Background Bitmap: $backgroundBitmap")

        return RenderOption().apply {
            content = if (ui.editInput.text.isEmpty()) "test qr" else ui.editInput.text.toString()
            size = 1000 // size of the final QR code image
            borderWidth = 20 // width of the empty space around the QR code
            ecl = ErrorCorrectionLevel.M // (optional) specify an error correction level
            patternScale = 0.35f // (optional) specify a scale for patterns
            roundedPatterns = true // (optional) if true, blocks will be drawn as dots instead
            clearBorder =
                true // if set to true, the background will NOT be drawn on the border area
            color = if (useRainbow) rainbowColor else Color()
            background = getBackground(backgroundType)
            logo = Logo().apply {
                bitmap = logoBitmap
                scale = 0.2f
                borderRadius = 8
                borderWidth = 10
                clippingRect = RectF(0F, 0F, 200F, 200F) // Use actual bitmap size
            }
        }


    }

    private fun getBackground(type: String): Background {

        val bitmapWidth = backgroundBitmap?.width ?: 200
        val bitmapHeight = backgroundBitmap?.height ?: 200
        val clippingRects = Rect(0, 0, bitmapWidth, bitmapHeight)

        return when (type) {
            "still" -> {
                StillBackground().apply {
                    bitmap = backgroundBitmap // assign a bitmap as the background
                    clippingRect = clippingRects
                    alpha = 0.7f // alpha of the background to be drawn
                }
            }

            "blend" -> {
                BlendBackground().apply {
                    bitmap = backgroundBitmap
                    clippingRect = clippingRects
                    alpha = 0.7f
                    borderRadius = 10 // radius for blending corners
                }
            }

            "gif" -> {
                GifBackground().apply {
                    inputFile = gifFile // assign a file object of a gif image to this field
                    outputFile = File(
                        pictureStorage, "output.gif"
                    ) // IMPORTANT: the output image will be saved to this file object
                    clippingRect = clippingRects
                    alpha = 0.7f
                }
            }

            else -> throw IllegalArgumentException("Invalid background type")
        }
    }


    private fun importDrawable(isLogo: Boolean, type: String = "still") {
        this.isLogo = isLogo
        backgroundType = type
        photoPickerLauncher.launch("image/*")
    }

}