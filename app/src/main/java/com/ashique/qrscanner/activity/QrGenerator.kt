package com.ashique.qrscanner.activity

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.toColorInt
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.ashique.qrscanner.R
import com.ashique.qrscanner.custom.Buttons
import com.ashique.qrscanner.databinding.QrGeneratorBinding
import com.ashique.qrscanner.helper.BitmapHelper.saveBitmap
import com.ashique.qrscanner.helper.Extensions.showToast
import com.ashique.qrscanner.helper.Extensions.toBitmapDrawable
import com.ashique.qrscanner.helper.QrHelper
import com.ashique.qrscanner.helper.QrHelper.scanBitmapRealtime
import com.ashique.qrscanner.helper.QrUiSetup.QrColorType
import com.ashique.qrscanner.services.ViewPagerAdapter
import com.bumptech.glide.Glide
import com.github.alexzhirkevich.customqrgenerator.HighlightingType
import com.github.alexzhirkevich.customqrgenerator.QrErrorCorrectionLevel
import com.github.alexzhirkevich.customqrgenerator.style.BitmapScale
import com.github.alexzhirkevich.customqrgenerator.style.QrShape
import com.github.alexzhirkevich.customqrgenerator.vector.QrCodeDrawable
import com.github.alexzhirkevich.customqrgenerator.vector.QrVectorOptions
import com.github.alexzhirkevich.customqrgenerator.vector.createQrVectorOptions
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorBallShape
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorColor
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorFrameShape
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorLogoPadding
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorLogoShape
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorPixelShape
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class QrGenerator : AppCompatActivity() {

    private val binding by lazy {
        QrGeneratorBinding.inflate(layoutInflater)
    }

    private lateinit var photoPickerLauncher: ActivityResultLauncher<String>

    private var isLogo = true
    private var btnDrawable: Buttons? = null

    companion object {

        var currentColorType: QrColorType? = null

        var qrBackground: BitmapDrawable? = null
        var qrLogo: BitmapDrawable? = null

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

        var centerCrop = false

        var selectedQrShape: QrShape = QrShape.Default

        var selectedDarkPixelShape: QrVectorPixelShape =
            QrVectorPixelShape.RoundCorners(darkPixelRoundness)

        var selectedFrameShape: QrVectorFrameShape = QrVectorFrameShape.RoundCorners(frameRoundness)

        var selectedEyeBallShape: QrVectorBallShape = QrVectorBallShape.RoundCorners(ballRoundness)

        var selectedGradientOrientation: QrVectorColor.LinearGradient.Orientation =
            QrVectorColor.LinearGradient
                .Orientation.Horizontal


    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)



        with(binding) {
            editInput.setText(R.string.app_name)

            updateQrCode()

            editInput.addTextChangedListener { updateQrCode() }

            /*
            Glide.with(this@QrGenerator)
                .asGif()
                .load("https://media.giphy.com/media/3oEjI6SIIHBdRxXI40/giphy.gif")
                .into(binding.gifView)


             */

            viewPagerShapes.adapter = ViewPagerAdapter(this@QrGenerator)

            navigationMenu.setViewPager(viewPagerShapes)


            photoPickerLauncher = registerForActivityResult(
                ActivityResultContracts.GetContent()
            ) { uri ->
                Glide.with(this@QrGenerator)
                    .asGif()
                    .load(uri)
                    .into(binding.gifView)
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



            btnSaveQr.setOnClickListener {
                binding.ivQrcode.drawable?.let { drawable ->
                    val bitmap = drawable.toBitmap(1024, 1024)
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

                    saveBitmap(bitmap, Bitmap.CompressFormat.PNG, "qr_code.png")
                } ?: run {
                    Toast.makeText(
                        this@QrGenerator, "QR code image is not available.", Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }


    }


    private fun createQrOptions(): QrVectorOptions {
        return createQrVectorOptions {
            padding = qrPadding
            fourthEyeEnabled = false
            errorCorrectionLevel = QrErrorCorrectionLevel.High
            var strokeWidth = 0f // Adjust stroke width
            val scaledSize = 0.8f // Adjust the scale factor to control the size of the square

            highlighting {
                cornerEyes = HighlightingType.Styled(
                    /* shape = { size, neighbors ->
                         Path().apply {
                             val path = Path()
                             // Calculate the offset to center the square
                             val offset = (size - scaledSize) / 2

                             // Draw a centered square
                             path.moveTo(offset, offset)
                             path.lineTo(offset + scaledSize, offset)
                             path.lineTo(offset + scaledSize, offset + scaledSize)
                             path.lineTo(offset, offset + scaledSize)
                             path.close()
                         }


                     },*/
                    shape = selectedFrameShape,
                    color = QrVectorColor.RadialGradient(
                        listOf(
                            0f to "#FF0000".toColorInt(),
                            1f to "#10000000".toColorInt(),
                        )
                    )
                )

                versionEyes = HighlightingType.Styled(
                    shape = selectedFrameShape,
                    color = QrVectorColor.RadialGradient(
                        listOf(
                            0f to "#FF0000".toColorInt(),
                            1f to "#10000000".toColorInt(),
                        )
                    )
                )
                timingLines = HighlightingType.Styled(color = QrVectorColor.Solid(Color.GREEN))

            }

            background {
                drawable = null //qrBackground
                color =
                    QrVectorColor.Solid(if (qrBackground == null) Color.TRANSPARENT else Color.TRANSPARENT)
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
                        ),
                        orientation = selectedGradientOrientation
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
                val bitmap =
                    QrCodeDrawable({ binding.editInput.text.toString() }, options).toBitmap(
                        400,
                        400,
                        Bitmap.Config.ARGB_8888
                    )

                // Switch to Main thread to update UI
                withContext(Dispatchers.Main) {
                    binding.ivQrcode.setImageBitmap(bitmap)


                }

                // Perform QR code scanning in the background


                scanBitmapRealtime(
                    bitmap,
                    onSuccess = { result ->
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
                    },
                    onFailure = {
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
                    }
                )


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


    fun importDrawable(isLogo: Boolean, button: Buttons) {
        this.isLogo = isLogo
        btnDrawable = button
        photoPickerLauncher.launch("image/*")
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