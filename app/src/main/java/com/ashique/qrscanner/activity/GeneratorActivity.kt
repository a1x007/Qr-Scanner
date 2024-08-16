package com.ashique.qrscanner.activity

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toBitmap
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.ashique.qrscanner.databinding.ActivityGeneratorBinding
import com.ashique.qrscanner.helper.BitmapHelper.saveBitmap
import com.ashique.qrscanner.helper.BitmapHelper.toPath
import com.ashique.qrscanner.helper.Extensions.createSeekBarListener
import com.bumptech.glide.Glide
import com.chaquo.python.Python
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File


class GeneratorActivity : AppCompatActivity() {

    private lateinit var ui: ActivityGeneratorBinding

    private var binaryConvert = false
    private var colorized = false
    private var isLogo = false

    private var qrUri: Uri? = null
    private var backgroundUri: Uri? = null

    private lateinit var photoPickerLauncher: ActivityResultLauncher<String>
    val TAG = "Generator"

    private var contrast = 2.0
    private var brightness = 1.5
    private var halftoneDotSize = 2
    private var halftoneResolution = 2


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = ActivityGeneratorBinding.inflate(layoutInflater)
        setContentView(ui.root)



        photoPickerLauncher = registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->

            backgroundUri = uri

            // uri?.let { convertToBinary(it) }


            uri?.let {
                if (isLogo) {
                    qrUri = it
                } else {
                    backgroundUri = it
                }


            }

        }

        ui.editInput.setText("Special,thus awesome.")
        ui.editInput.addTextChangedListener { }
        ui.generateBtn.setOnClickListener {
            if (binaryConvert) backgroundUri?.let { uri ->
                convertToBinary(
                    uri
                )
            } else combine(qrUri, backgroundUri)
        }
        ui.uploadLogoBtn.setOnClickListener { importDrawable(colorized, isLogo = true) }
        ui.uploadStillBgBtn.setOnClickListener { importDrawable(colorized) }
        ui.uploadBlendBgBtn.setOnClickListener { importDrawable(colorized = true) }
        ui.uploadGifBgBtn.setOnClickListener { importDrawable(colorized = false) }
        ui.colorizedSwitch.setOnCheckedChangeListener { _, isChecked -> colorized = isChecked }
        ui.binarySwitch.setOnCheckedChangeListener { _, isChecked -> binaryConvert = isChecked }


        ui.halftoneDotsizeSlider.setOnSeekBarChangeListener(
            createSeekBarListener(onProgressChanged = { progress ->
                halftoneDotSize = progress
                ui.halftoneDotsizeText.text = String.format("Dot Size %d", halftoneDotSize)


            }, onStop = { hasStopped ->
                if (hasStopped) {
                    backgroundUri?.let { convertToBinary(it) }
                }

            })
        )

        ui.halftoneResolutionSlider.setOnSeekBarChangeListener(
            createSeekBarListener(onProgressChanged = { progress ->
                halftoneResolution = progress
                ui.halftoneResolutionText.text = String.format("Resolution %d", halftoneResolution)

            },
                onStop = { hasStopped ->
                    if (hasStopped) {
                        backgroundUri?.let { convertToBinary(it) }
                    }

                })
        )

        ui.contrastSlider.setOnSeekBarChangeListener(createSeekBarListener(onProgressChanged = { progress ->
            contrast = (progress / 100.toDouble()) * 2.0
            ui.contrastText.text = String.format("Contrast %.2f", contrast)


        }, onStop = { hasStopped ->
            if (hasStopped) {
                backgroundUri?.let { convertToBinary(it) }
            }

        }))

        ui.brightnessSlider.setOnSeekBarChangeListener(
            createSeekBarListener(onProgressChanged = { progress ->
                brightness = (progress / 100.toDouble()) * 2.0
                ui.brightnessText.text = String.format("Brightness %.2f", brightness)

            }, onStop = { hasStopped ->
                if (hasStopped) {
                    backgroundUri?.let { convertToBinary(it) }
                }

            })
        )

        ui.saveBtn.setOnClickListener {
            ui.qrPreview.drawable?.let { drawable ->
                val bitmap = drawable.toBitmap(1024, 1024)
                saveBitmap(bitmap, Bitmap.CompressFormat.JPEG, "qr_code.png")
            } ?: run {
                Toast.makeText(
                    this, "QR code image is not available.", Toast.LENGTH_SHORT
                ).show()
            }
        }
    }


    private fun convertToBinary(uri: Uri) {
        // Start a coroutine for background processing
        lifecycleScope.launch(Dispatchers.IO) {
            val inputStream = contentResolver.openInputStream(uri)
            val isGif = inputStream?.use {
                val header = ByteArray(6)
                it.read(header)
                header[0] == 'G'.code.toByte() && header[1] == 'I'.code.toByte() && header[2] == 'F'.code.toByte()
            } ?: false

            val extension = if (isGif) "gif" else "png"
            val directory = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "Qr"
            )
            if (!directory.exists()) {
                directory.mkdirs()  // Create the directory if it doesn't exist
            }

            val outputGifFile = File(directory, "${System.currentTimeMillis()}_binary.$extension")
            val outputGifPath = outputGifFile.absolutePath
            Log.i(TAG, "convertToBinary: outputpath: $outputGifPath")

            if (!outputGifFile.exists()) {
                // Get the Python instance
                val python = Python.getInstance()

                // Call the Python script to convert the image to binary
                python.getModule("convert").callAttr(
                    "convert_to_binary",
                    uri.toPath(this@GeneratorActivity),
                    outputGifPath,
                    colorized,
                    contrast,
                    brightness,
                    isGif,
                    "ultra",
                    halftoneDotSize,
                    halftoneResolution

                )
            }

            if (outputGifFile.exists()) {
                withContext(Dispatchers.Main) {
                    Glide.with(this@GeneratorActivity).load(outputGifPath).into(ui.qrPreview)
                }
            } else {
                withContext(Dispatchers.Main) {
                    ui.qrPreview.setImageBitmap(null) // Clear the ImageView or show an error image
                    Toast.makeText(
                        this@GeneratorActivity,
                        "Conversion failed. Output file not found.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }


    private fun combine(qrUri: Uri?, bgUri: Uri?) {
        if (qrUri != null && bgUri != null) {
            // Start a coroutine for background processing
            lifecycleScope.launch(Dispatchers.IO) {
                val inputStream = contentResolver.openInputStream(bgUri)
                val isGif = inputStream?.use {
                    val header = ByteArray(6)
                    it.read(header)
                    header[0] == 'G'.code.toByte() && header[1] == 'I'.code.toByte() && header[2] == 'F'.code.toByte()
                } ?: false

                val extension = if (isGif) "gif" else "png"
                val directory = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "Qr"
                )
                if (!directory.exists()) {
                    directory.mkdirs()  // Create the directory if it doesn't exist
                }

                val outputGifFile =
                    File(directory, "${System.currentTimeMillis()}_binary.$extension")
                val outputGifPath = outputGifFile.absolutePath
                Log.i(TAG, "convertToBinary: outputpath: $outputGifPath")

                if (!outputGifFile.exists()) {
                    // Get the Python instance
                    val python = Python.getInstance()

                    // Call the Python script to convert the image to binary
                    python.getModule("convert").callAttr(
                        "convert_to_binary",
                        qrUri.toPath(this@GeneratorActivity),
                        bgUri.toPath(this@GeneratorActivity),
                        outputGifPath,
                        colorized,
                        contrast,
                        brightness,
                        isGif,
                        "ultra",
                        halftoneDotSize,
                        halftoneResolution

                    )
                }

                if (outputGifFile.exists()) {
                    withContext(Dispatchers.Main) {
                        Glide.with(this@GeneratorActivity).load(outputGifPath).into(ui.qrPreview)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        ui.qrPreview.setImageBitmap(null) // Clear the ImageView or show an error image
                        Toast.makeText(
                            this@GeneratorActivity,
                            "Conversion failed. Output file not found.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }


    private fun importDrawable(colorized: Boolean, isLogo: Boolean = false) {
        this.colorized = colorized
        this.isLogo = isLogo
        photoPickerLauncher.launch("image/*")
    }

}