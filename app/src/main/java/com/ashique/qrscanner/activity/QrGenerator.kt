package com.ashique.qrscanner.activity

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.ashique.qrscanner.R
import com.ashique.qrscanner.custom.Buttons
import com.ashique.qrscanner.databinding.QrGeneratorBinding
import com.ashique.qrscanner.helper.BitmapHelper.saveBitmap
import com.ashique.qrscanner.helper.Extensions.isGifUri
import com.ashique.qrscanner.helper.Extensions.showToast
import com.ashique.qrscanner.helper.Extensions.toBitmapDrawable
import com.ashique.qrscanner.helper.Extensions.toFile
import com.ashique.qrscanner.helper.GifPipeline
import com.ashique.qrscanner.helper.QrHelper
import com.ashique.qrscanner.helper.QrHelper.scanBitmapRealtime
import com.ashique.qrscanner.helper.QrUiSetup.QrColorType
import com.ashique.qrscanner.services.ViewPagerAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.github.alexzhirkevich.customqrgenerator.HighlightingType
import com.github.alexzhirkevich.customqrgenerator.QrErrorCorrectionLevel
import com.github.alexzhirkevich.customqrgenerator.style.BitmapScale
import com.github.alexzhirkevich.customqrgenerator.style.QrShape
import com.github.alexzhirkevich.customqrgenerator.vector.QrCodeDrawable
import com.github.alexzhirkevich.customqrgenerator.vector.QrVectorOptions
import com.github.alexzhirkevich.customqrgenerator.vector.createQrVectorOptions
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorBackground
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorBallShape
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorColor
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorFrameShape
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorLogoPadding
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorLogoShape
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorPixelShape
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class QrGenerator : AppCompatActivity() {

    private val binding by lazy {
        QrGeneratorBinding.inflate(layoutInflater)
    }

    private lateinit var photoPickerLauncher: ActivityResultLauncher<String>

    private var isLogo = true
    private var btnDrawable: Buttons? = null
    var gifBackground: Uri? = null
    val TAG = "QrGenerator"

    companion object {

        var currentColorType: QrColorType? = null


        var qrBackground: BitmapDrawable? = null
        var qrLogo: BitmapDrawable? = null
        var qrBackgroundColor = Color.TRANSPARENT
        private var defaultColor = Color.BLACK
        var ballColor = defaultColor
        var frameColor = defaultColor
        var darkColor = defaultColor

        var drawableBgColor = Color.RED

        var useSolidColor = true
        var useLinearGradient = false
        var useRadialGradient = false
        var useSweepGradient = false

        var gradientColor0 = Color.RED
        var gradientColor1 = Color.GREEN

        var qrPadding: Float = .100f
        var logoPadding: Float = .2f
        var logoSize: Float = .25f
        var darkPixelRoundness: Float = 0.0f
        var ballRoundness: Float = .25f
        var frameRoundness: Float = .25f

        var centerCrop = true

        var selectedQrShape: QrShape = QrShape.Default

        var selectedDarkPixelShape: QrVectorPixelShape =
            QrVectorPixelShape.RoundCorners(darkPixelRoundness)

        var selectedFrameShape: QrVectorFrameShape = QrVectorFrameShape.RoundCorners(frameRoundness)

        var selectedEyeBallShape: QrVectorBallShape = QrVectorBallShape.RoundCorners(ballRoundness)

        var selectedGradientOrientation: QrVectorColor.LinearGradient.Orientation =
            QrVectorColor.LinearGradient.Orientation.Horizontal


    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)



        with(binding) {
            editInput.setText(R.string.app_name)

            updateQrCode()

            editInput.addTextChangedListener { updateQrCode() }



            viewPagerShapes.adapter = ViewPagerAdapter(this@QrGenerator)

            navigationMenu.setViewPager(viewPagerShapes)


            photoPickerLauncher = registerForActivityResult(
                ActivityResultContracts.GetContent()
            ) { uri ->
                if (uri?.isGifUri(this@QrGenerator) == true) {
                    Log.i("", "onCreate: gif file detected")
                    gifBackground = uri
                    qrBackground = null
                    Glide.with(this@QrGenerator).load(uri).diskCacheStrategy(DiskCacheStrategy.ALL)
                        .transition(DrawableTransitionOptions.withCrossFade()).into(binding.gifView)
                }
                uri?.toBitmapDrawable(this@QrGenerator)?.let { drawable ->
                    if (isLogo) {
                        qrLogo = drawable
                    } else {
                        qrBackground = drawable

                    }
                    btnDrawable?.isChecked = false
                    updateQrCode()

                }


            }

            btnSaveGif.setOnClickListener { generateGif() }


            btnSaveQr.setOnClickListener {
                val bitmap = QrCodeDrawable(
                    { binding.editInput.text.toString() },
                    createQrOptions()
                ).toBitmap(1024, 1024, Bitmap.Config.ARGB_8888)

                if (QrHelper.scanQrCode(bitmap).isNullOrEmpty()) {
                    verifyQrText.apply {
                        setTextColor(Color.RED)
                        text = String.format("Qr is corrupted !")
                    }

                } else {
                    verifyQrText.apply {
                        setTextColor(Color.GREEN)
                        text = String.format("Qr is verified.")
                    }

                }
                Log.d("QRCodeDebug", "Bitmap Transparency: ${bitmap.hasAlpha()}")

                saveBitmap(bitmap, Bitmap.CompressFormat.PNG, "qrcode_${System.currentTimeMillis()}.png")

            }
        }


    }


    private fun createQrOptions(): QrVectorOptions {
        return createQrVectorOptions {
            padding = qrPadding
            /** Should be from 0 to 0.5. Default value is 0 */
            fourthEyeEnabled = false
            errorCorrectionLevel = QrErrorCorrectionLevel.High


            highlighting {
                cornerEyes = HighlightingType.Styled(
                    shape = selectedFrameShape, color = QrVectorColor.Solid(Color.WHITE)
                )

                versionEyes = HighlightingType.Styled(
                    shape = QrVectorFrameShape.Circle(50f), color = QrVectorColor.Solid(Color.WHITE)
                )

                timingLines = HighlightingType.Styled(
                    shape = QrVectorPixelShape.Square(.25f),
                    color = QrVectorColor.Solid(Color.WHITE)
                )
                alpha = 1f
            }

            background {
                drawable = if (gifBackground == null) qrBackground else null
                color =
                    if (gifBackground != null) QrVectorColor.Solid(Color.TRANSPARENT) else QrVectorColor.Solid(
                        if (qrBackground == null) qrBackgroundColor else Color.TRANSPARENT
                    )
                codeShape = selectedQrShape
                scale = if (centerCrop) BitmapScale.CenterCrop else BitmapScale.FitXY
            }

            logo {
                drawable = qrLogo
                size = logoSize
                padding = QrVectorLogoPadding.Natural(logoPadding)
                shape = QrVectorLogoShape.Circle

            }

            colors {
                light = QrVectorColor.Solid(Color.WHITE)

                dark = if (useLinearGradient) {
                    QrVectorColor.LinearGradient(
                        colors = listOf(
                            0f to gradientColor0,
                            1f to gradientColor1,
                        ), orientation = selectedGradientOrientation
                    )
                } else if (useRadialGradient) {
                    QrVectorColor.RadialGradient(
                        colors = listOf(
                            0f to gradientColor0,
                            1f to gradientColor1,
                        ),

                        )
                } else if (useSweepGradient) {
                    QrVectorColor.SweepGradient(
                        colors = listOf(
                            0f to gradientColor0,
                            1f to gradientColor1,
                        ),
                    )
                } else QrVectorColor.Solid(darkColor)

                if (!useLinearGradient && !useSweepGradient && !useRadialGradient) {
                    ball = QrVectorColor.Solid(ballColor)
                    frame = QrVectorColor.Solid(frameColor)
                }
            }
            shapes {
                lightPixel = selectedDarkPixelShape
                darkPixel = selectedDarkPixelShape
                ball = selectedEyeBallShape
                frame = selectedFrameShape
            }
        }
    }


    fun updateQrCode() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Generate QR code bitmap in a background thread
                val options = createQrOptions()
                val qrCOde = QrCodeDrawable({ binding.editInput.text.toString() }, options)
                val bitmap = qrCOde.toBitmap(
                    400, 400, Bitmap.Config.ARGB_8888
                )


                // Switch to Main thread to update UI
                withContext(Dispatchers.Main) {
                    binding.ivQrcode.setImageBitmap(bitmap)

                }

                // Perform QR code scanning in the background
                scanBitmapRealtime(bitmap, onSuccess = { result ->
                    if (result != null) {
                        lifecycleScope.launch(Dispatchers.Main) {
                            with(binding) {
                                verifyQrText.apply {
                                    setTextColor(Color.GREEN)
                                    text = "Qr is verified."
                                }
                            }
                        }
                        println("Scanned QR code: $result")
                    } else {
                        // Handle case where nothing was found
                        lifecycleScope.launch(Dispatchers.Main) {
                            with(binding) {
                                verifyQrText.apply {
                                    setTextColor(Color.RED)
                                    text = "Qr is corrupted!"
                                }
                            }
                        }
                        println("No QR code found in the image.")
                    }
                }, onFailure = {
                    // Handle failure (this block is optional)
                    lifecycleScope.launch(Dispatchers.Main) {
                        with(binding) {
                            verifyQrText.apply {
                                setTextColor(Color.RED)
                                text = "Qr is corrupted!"
                            }
                        }
                    }
                    println("Failed to scan the QR code.")
                })


                /*   QrHelper.scanBitmap(bitmap, object : QrHelper.QrScanCallback {
                       override fun onBarcodeScanned(contents: String) {
                           // Switch to Main thread to update UI
                           lifecycleScope.launch(Dispatchers.Main) {
                               with(binding) {
                                   if (contents.isNotEmpty()) {
                                       verifyQrText.apply {
                                           setTextColor(Color.GREEN)
                                           text = "Qr is verified."
                                       }
                                   }
                               }
                           }
                       }

                       override fun onScanError(errorMessage: String) {
                           // Handle the error on the Main thread
                           lifecycleScope.launch(Dispatchers.Main) {
                               with(binding) {
                                   verifyQrText.apply {
                                       setTextColor(Color.RED)
                                       text = "Qr is corrupted!"
                                   }
                               }
                           }
                       }
                   })


                 */

            } catch (e: Exception) {
                // Handle any exceptions that occur during QR code generation or scanning
                withContext(Dispatchers.Main) {
                    showToast("Error: ${e.message}")
                }
            }


        }


    }


    fun generateGifQr(gifUri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Convert URI to File
                val gifFile = toFile(gifUri)
                if (gifFile == null) {
                    println("Failed to convert URI to File.")
                    return@launch
                }

                // Create an instance of GifPipeline
                val gifPipeline = GifPipeline()

                // Set the clipping rect to match the QR bitmaps dimensions
                gifPipeline.clippingRect = RectF(
                    0F, 0F, 1024F, 1024F
                )

                // Re-initialize the GIF pipeline after setting the clipping rect
                if (!gifPipeline.init(gifFile)) {
                    println("Failed to initialize GIF pipeline: ${gifPipeline.errorInfo}")
                    return@launch
                }

                val qrFrames = mutableListOf<Bitmap>()
                var frameCount = 0

                // Process each frame from the GIF
                while (true) {
                    val frame = gifPipeline.nextFrame() ?: break
                    // Log the frame processing
                    println("Processing frame ${++frameCount}")

                    val modifiedOptions = createQrOptions().copy(
                        background = QrVectorBackground(
                            drawable = frame.toDrawable(resources)
                        )
                    )

                    val qr = QrCodeDrawable({ binding.editInput.text.toString() }, modifiedOptions)
                    val bitmap = qr.toBitmap(1024, 1024, Bitmap.Config.ARGB_8888)

                    qrFrames.add(bitmap)
                    frame.recycle() // Free up memory
                }

                // Log the number of frames processed
                println("Number of frames processed: ${qrFrames.size}")

                // Check if any frames were processed
                if (qrFrames.isEmpty()) {
                    println("No frames were processed.")
                    return@launch
                }

                // Output file for GIF encoding
                val outputGifFile = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
                    "qrcode_${System.currentTimeMillis()}.gif"
                )

                // Set output file for GIF encoding
                gifPipeline.outputFile = outputGifFile
                qrFrames.forEach { gifPipeline.pushRendered(it) }

                // Encode frames to GIF
                if (!gifPipeline.postRender()) {
                    println("Failed to render GIF: ${gifPipeline.errorInfo}")
                } else {
                    if (gifFile.exists()) {
                        gifFile.delete()
                    }
                    println("GIF processed successfully! output: ${gifPipeline.outputFile}")
                }

            } catch (e: Exception) {
                e.printStackTrace()
                println("Error occurred: ${e.message}")
            }
        }
    }


    fun generateGifQr1(gifUri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Convert URI to file
                val gifFile = toFile(gifUri) ?: run {
                    Log.e(TAG, "generateGifQr1: GIF file doesn't exist.")
                    return@launch
                }

                // Initialize GIF pipeline
                val gifPipeline = GifPipeline().apply {
                    if (!init(gifFile)) {
                        Log.e(TAG, "Failed to initialize GIF pipeline: $errorInfo")
                        return@launch
                    }
                }

                // Prepare output file
                val outputGifFile = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
                    "qrcode_${System.currentTimeMillis()}.gif"
                )

                gifPipeline.outputFile = outputGifFile


                // QR code generator function
                val qrCodeGenerator: (Bitmap) -> Bitmap = { frame ->
                    val options = createQrOptions().copy(
                        background = QrVectorBackground(
                            drawable = frame.toDrawable(resources), scale = BitmapScale.CenterCrop
                        )

                    )
                    QrCodeDrawable({ binding.editInput.text.toString() }, options).toBitmap(
                        1024, 1024, Bitmap.Config.ARGB_8888
                    )
                }

                // Process frames
                var frameCount = 0
                var frame: Bitmap?
                while (true) {
                    frame = gifPipeline.nextFrame(convertBinary = false) ?: break
                    Log.d(TAG, "Processing frame ${++frameCount}")

                    // Generate QR code bitmap and push to pipeline
                    val qrBitmap = qrCodeGenerator(frame)
                    gifPipeline.pushRendered(qrBitmap)

                    // Recycle frame to free memory
                    frame.recycle()
                }

                // Finalize GIF processing
                if (frameCount == 0) {
                    Log.w(TAG, "No frames were processed.")
                } else {
                    if (!gifPipeline.postRender()) {
                        Log.e(TAG, "Failed to render GIF: ${gifPipeline.errorInfo}")
                    } else {
                        gifFile.delete() // delete the original GIF file
                        Log.i(TAG, "GIF processed successfully! Output: ${gifPipeline.outputFile}")
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error occurred: ${e.message}", e)
            }
        }
    }


    fun importDrawable(isLogo: Boolean, button: Buttons) {
        this.isLogo = isLogo
        btnDrawable = button
        gifBackground = null
        qrBackground = null
        photoPickerLauncher.launch("image/*")
    }


    fun generateGif() {
        gifBackground?.let { generateGifQr1(it) }
    }

}


/*  QrVectorFrameShape.AsDarkPixels
               QrVectorFrameShape.Circle(.2f)
               QrVectorFrameShape.AsPixelShape(QrVectorPixelShape.Circle())
               QrVectorPixelShape.Default
               QrVectorPixelShape.Circle()
               QrVectorPixelShape.RoundCorners(.25f)
               QrVectorPixelShape.Star
               QrVectorPixelShape.Rhombus()
               QrVectorPixelShape.RoundCornersHorizontal()
               QrVectorPixelShape.RoundCornersVertical()

              */