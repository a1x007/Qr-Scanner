package com.ashique.qrscanner.activity

import android.graphics.Bitmap
import android.graphics.Color
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
import com.ashique.qrscanner.helper.QrScanner.scanQrBitmap
import com.ashique.qrscanner.helper.QrUiSetup
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

    val TAG = "QrGenerator"

    companion object {

        var currentColorType: QrColorType? = null

        var gifBackground: Uri? = null
        var originalBackground: BitmapDrawable? = null
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

        //background image
        var brightness: Float = 0f
        var contrast: Float = 0f

        var useBinary = false
        var useHalftone = false
        var dotSize = 1
        var colorize = false
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
                uri?.let {
                    when {
                        it.isGifUri(this@QrGenerator) -> {
                            Log.i("QRCodeDebug", "GIF file detected")
                            gifBackground = it
                            qrBackground = null
                            Glide.with(this@QrGenerator)
                                .load(it)
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .transition(DrawableTransitionOptions.withCrossFade())
                                .into(binding.gifView)
                        }

                        else -> {
                            val drawable = it.toBitmapDrawable(this@QrGenerator)
                            drawable?.let { image ->
                                if (isLogo) {
                                    qrLogo = image
                                } else {
                                    qrBackground = image
                                    originalBackground = image
                                    gifBackground = null
                                }
                                btnDrawable?.isChecked = false
                                updateQrCode()
                            }
                        }
                    }
                }
            }


            btnSaveGif.setOnClickListener { generateGif() }


            btnSaveQr.setOnClickListener {
                // Generate the QR code bitmap
                val bitmap = QrCodeDrawable(
                    { binding.editInput.text.toString() },
                    createQrOptions()
                ).toBitmap(1024, 1024, Bitmap.Config.ARGB_8888)

                // Start the coroutine in the appropriate lifecycle scope
                lifecycleScope.launch(Dispatchers.IO) {
                    // Scan the generated QR code to verify its integrity
                    val qrScanResult = scanQrBitmap(bitmap)

                    withContext(Dispatchers.Main) {
                        // Update the UI based on the scan result
                        if (qrScanResult.isNullOrEmpty()) {
                            verifyQrText.apply {
                                setTextColor(Color.RED)
                                text = "QR is corrupted!"
                            }
                        } else {
                            verifyQrText.apply {
                                setTextColor(Color.GREEN)
                                text = "QR is verified."
                            }
                        }
                    }

                    // Save the bitmap after verifying
                    saveBitmap(
                        bitmap,
                        Bitmap.CompressFormat.PNG,
                        "qrcode_${System.currentTimeMillis()}.png"
                    )

                    // Debugging: Log bitmap transparency
                    Log.d("QRCodeDebug", "Bitmap Transparency: ${bitmap.hasAlpha()}")
                }
            }

        }


    }


    private fun createQrOptions(): QrVectorOptions {
        return createQrVectorOptions {
            /** Should be from 0 to 0.5. Default value is 0 */
            padding = qrPadding
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
                drawable = qrBackground.takeIf { gifBackground == null }
                color = QrVectorColor.Solid(
                    when {
                        gifBackground != null -> Color.TRANSPARENT
                        qrBackground != null -> Color.TRANSPARENT
                        else -> qrBackgroundColor
                    }
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
                val qrText = scanQrBitmap(bitmap)
                withContext(Dispatchers.Main) {
                    if (qrText != null) {

                        binding.verifyQrText.apply {
                            setTextColor(Color.GREEN)
                            text = "QR is verified."
                        }
                    } else {
                        Log.e(TAG, "updateQrCode: qr is corrupted.")
                        binding.verifyQrText.apply {
                            setTextColor(Color.RED)
                            text = "QR is corrupted!"
                        }
                    }
                }

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
                    frame = gifPipeline.nextFrame(
                        brightness = brightness,
                        contrast = contrast,
                        halftone = useHalftone,
                        binary = useBinary,
                        colorize = colorize,
                        dotSize = dotSize
                    ) ?: break
                    Log.d(TAG, "Processing frame ${++frameCount} / useColor: $colorize")

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
        gifBackground?.let { generateGifQr(it) }
    }

    fun saveQrCode(format: QrUiSetup.QrFormats) {
        // Generate the QR code bitmap
        val bitmap = QrCodeDrawable(
            { binding.editInput.text.toString() },
            createQrOptions()
        ).toBitmap(1024, 1024, Bitmap.Config.ARGB_8888)


        when (format) {
            QrUiSetup.QrFormats.JPG -> {
                saveBitmap(
                    bitmap,
                    Bitmap.CompressFormat.JPEG,
                    "qrcode_${System.currentTimeMillis()}.jpg"
                )
            }

            QrUiSetup.QrFormats.PNG -> {
                saveBitmap(
                    bitmap,
                    Bitmap.CompressFormat.PNG,
                    "qrcode_${System.currentTimeMillis()}.png"
                )
            }

            QrUiSetup.QrFormats.GIF -> {
                gifBackground?.let { generateGifQr(it) }
            }
        }
    }


}
