package com.ashique.qrscanner.activity

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.RectF
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.lifecycleScope
import com.ashique.qrscanner.databinding.ActivitySettingsBinding
import com.ashique.qrscanner.helper.BitmapHelper.saveBitmap
import com.ashique.qrscanner.helper.BitmapHelper.toBitmap
import com.ashique.qrscanner.helper.BitmapHelper.toInputStream
import com.ashique.qrscanner.helper.BitmapHelper.toMutableBitmap
import com.ashique.qrscanner.helper.Combine.convertToBinaryBitmap
import com.ashique.qrscanner.helper.Combine.convertToDotBinaryBitmap
import com.ashique.qrscanner.helper.Combine.generateQrCodeWithBinaryBitmap
import com.ashique.qrscanner.helper.GifPipeline
import com.ashique.qrscanner.helper.GifPipeline2
import com.ashique.qrscanner.helper.Prefs.initialize
import com.ashique.qrscanner.helper.Prefs.setZxing
import com.ashique.qrscanner.helper.Prefs.useZxing
import com.ashique.qrscanner.helper.QrHelper
import com.bumptech.glide.Glide
import com.chaquo.python.Python
import com.github.sumimakito.awesomeqr.option.background.GifBackground
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.InputStream

class SettingsActivity : AppCompatActivity() {

    private lateinit var ui: ActivitySettingsBinding

    private lateinit var photoPickerLauncher: ActivityResultLauncher<String>

    var qrCode: String? = null
    private var qrUri: Uri? = null
    private var bgUri: Uri? = null

    private val pickQrImage =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            qrUri = uri
            // ui.preview.setImageBitmap(qrUri?.toBitmap(this)?.toMutableBitmap()?.let { convertToBinaryBitmap(it) })
          //  checkAndCombineImages()
            gifBackground()
        }

    private val pickBgImage =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            bgUri = uri
           // checkAndCombineImages()
            gifBackground()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(ui.root)

        initialize(this) // Prefs

        ui.switchZxing.isChecked = useZxing()

        ui.switchZxing.setOnCheckedChangeListener { _, isChecked ->
            setZxing(isChecked)
        }

        photoPickerLauncher = registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->
            uri.let {
                qrCode = generateQrCode(
                    data = "testing pic qr",
                    version = 10,
                    level = "H",
                    pictureUri = uri,
                    colorized = true,
                    saveName = "qr_${System.currentTimeMillis()}.png"
                )

                val bitmap = BitmapFactory.decodeFile(qrCode)
                ui.preview.setImageBitmap(bitmap)
            }

        }

        ui.upload.setOnClickListener {
            photoPickerLauncher.launch("image/*")
        }

        ui.uploadBg.setOnClickListener { pickBgImage() }
        ui.uploadQr.setOnClickListener { pickQrImage() }

        ui.saveBtn.setOnClickListener {
            ui.preview.drawable?.let { drawable ->
                val bitmap = drawable.toBitmap(1024, 1024)
                if (QrHelper.scanQrCode(bitmap).isNullOrEmpty()) {
                    ui.verifyQrText.apply {
                        setTextColor(Color.RED)
                        text = String.format("Qr is corrupted !")
                    }

                } else {
                    ui.verifyQrText.apply {
                        setTextColor(Color.GREEN)
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


    fun generateQrCode(
        data: String,
        version: Int = 1,
        level: String = "H",
        pictureUri: Uri? = null,
        colorized: Boolean = false,
        contrast: Float = 1.0f,
        brightness: Float = 1.0f,
        saveName: String
    ): String {
        val python = Python.getInstance()
        val pythonModule = python.getModule("qr_generator")
        // Convert URI to file path if it's not null
        val picturePath = pictureUri?.let { uriToFile(this, it)?.absolutePath }
        val result = pythonModule.callAttr(
            "generate_qr",
            data,
            version,
            level,
            picturePath,
            colorized,
            contrast,
            brightness,
            saveName
        )
        return result.toString()
    }

    fun uriToFile(context: Context, uri: Uri): File? {
        val contentResolver: ContentResolver = context.contentResolver
        var file: File? = null
        try {
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            if (inputStream != null) {
                val tempFile = File.createTempFile("temp_image", ".png", context.cacheDir)
                file = tempFile
                file.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return file
    }

    private fun checkAndCombineImages() {
        if (qrUri != null && bgUri != null) {
            val qr = qrUri?.toBitmap(this)?.toMutableBitmap()
            val bg = bgUri?.toBitmap(this)?.toMutableBitmap()

            lifecycleScope.launch(Dispatchers.IO) {
                val bitmap =
                    convertToBinaryBitmap(qr = qr!!, original = bg!!, threshold = 100)
                val dotMatrix = convertToDotBinaryBitmap(qr = qr!!, original = bg!!, dotSize = 2)
                val qrCodeBitmap = generateQrCodeWithBinaryBitmap("Your data here", bg)

                Log.i("", "checkAndCombineImages: $bitmap")
                // saveBitmap(bitmap, Bitmap.CompressFormat.PNG, "${System.currentTimeMillis()}.png")

                withContext(Dispatchers.Main) {
                    ui.preview.setImageBitmap(qrCodeBitmap)
                }
            }
            //  ui.preview.generateAndOverlayQrCode(qrBitmap = qr!!, bgBitmap = bg!!)
            /*  ui.preview.loadAndCombineQr(
                  qrUri = qrUri!!,
                  bgUri = bgUri!!,
                  colorized = true,
                  contrast = 1.5f,
                  brightness = 1.2f,
                  saveName = "final_qr.png"
              )

             */
        }
    }

    // Call these functions to start the image picking process
    private fun pickQrImage() {
        pickQrImage.launch("image/*")
    }

    private fun pickBgImage() {
        pickBgImage.launch("image/*")
    }

    fun gifBackground() {
        if (qrUri != null && bgUri != null) {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    // Initialize GifPipeline
                    val gifPipeline = GifPipeline2()

                    // Convert bgUri to InputStream
                    val bgInputStream = bgUri!!.toInputStream(this@SettingsActivity)
                    if (bgInputStream != null) {
                        val gifFile = File.createTempFile("temp_gif", ".gif", this@SettingsActivity.cacheDir)
                        Log.i("TAG", "gifBackground: $bgInputStream / $gifFile")
                        bgInputStream.copyTo(gifFile.outputStream())
                        if (gifPipeline.init(gifFile)) {
                            // Convert qrUri to a Bitmap and set it as the QR bitmap
                            val qrBitmap = qrUri!!.toBitmap(this@SettingsActivity)?.toMutableBitmap()
                            if (qrBitmap != null) {
                                gifPipeline.qrBitmap = qrBitmap
                                gifPipeline.clippingRect = RectF(0f, 0f, qrBitmap.width.toFloat(), qrBitmap.height.toFloat()) // Set dimensions as needed

                                Log.i("TAG", "gifBackground: qrBitmap: $qrBitmap")
                            } else {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(this@SettingsActivity, "Failed to load QR Bitmap", Toast.LENGTH_SHORT).show()
                                }
                                return@launch
                            }

                            // Process each frame of the GIF
                            while (true) {
                                val frame = gifPipeline.nextFrame() ?: break
                                gifPipeline.pushRendered(frame)
                                Log.i("TAG", "gifBackground: processing frame: $frame")
                            }

                            // Define the output file path
                            val outputGifFile = File(
                                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
                                "demo.gif"
                            )
                            gifPipeline.outputFile = outputGifFile
                            Log.i("TAG", "gifBackground: outPut: ${gifPipeline.outputFile}")

                            // Render the final GIF and load it into the ImageView
                            if (gifPipeline.postRender()) {
                                Log.i("TAG", "gifBackground: loading the gif to imageView")
                                withContext(Dispatchers.Main) {
                                    Glide.with(this@SettingsActivity)
                                        .asGif()
                                        .load(outputGifFile)
                                        .into(ui.preview) // Replace with your ImageView instance
                                }
                            } else {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(this@SettingsActivity, "Failed to render GIF", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@SettingsActivity, "Failed to initialize GIF", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@SettingsActivity, "Failed to get background InputStream", Toast.LENGTH_SHORT).show()
                        }
                    }

                    // Release resources
                    gifPipeline.release()
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@SettingsActivity, "An error occurred: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                    e.printStackTrace()
                }
            }
        } else {
            Toast.makeText(this, "QR Uri or Background Uri is null", Toast.LENGTH_SHORT).show()
        }
    }


 /*fun gifRender(){
    val background = bgUri!!.toInputStream(this@SettingsActivity)
        ?: throw Exception("Output file has not yet been set. It is required under GIF background mode.")
    val gifPipeline = GifPipeline()
    if (background != null) {
        val gifFile = File.createTempFile("temp_gif", ".gif", this@SettingsActivity.cacheDir)
        Log.i("TAG", "gifBackground: $background / $gifFile")
        background.copyTo(gifFile.outputStream())

    if (!gifPipeline.init(gifFile)) {
        throw Exception("GifPipeline failed to init: " + gifPipeline.errorInfo)
    }
        val qrBitmap = qrUri!!.toBitmap(this@SettingsActivity)?.toMutableBitmap()
        if (qrBitmap != null) {
            gifPipeline = qrBitmap

        gifPipeline.clippingRect = RectF(0f, 0f, qrBitmap.width.toFloat(), qrBitmap.height.toFloat()) // Set dimensions as needed
    gifPipeline.outputFile = background.outputFile
    var frame: Bitmap?
    var renderedFrame: Bitmap
    var firstRenderedFrame: Bitmap? = null
    frame = gifPipeline.nextFrame()
    while (frame != null) {
        renderedFrame = renderFrame(renderOptions, frame)
        gifPipeline.pushRendered(renderedFrame)
        if (firstRenderedFrame == null) {
            firstRenderedFrame = renderedFrame.copy(Bitmap.Config.ARGB_8888, true)
        }
        frame = gifPipeline.nextFrame()
    }
    if (gifPipeline.errorInfo != null) {
        throw Exception("GifPipeline failed to render frames: " + gifPipeline.errorInfo)
    }
    if (!gifPipeline.postRender()) {
        throw Exception("GifPipeline failed to do post render works: " + gifPipeline.errorInfo)
    }
    return RenderResult(firstRenderedFrame, background.outputFile, RenderResult.OutputType.GIF)
}


  */
}
