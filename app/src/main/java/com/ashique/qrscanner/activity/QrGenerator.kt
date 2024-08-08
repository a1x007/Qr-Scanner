package com.ashique.qrscanner.activity

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.lifecycleScope
import com.ashique.qrscanner.R
import com.ashique.qrscanner.databinding.QrGeneratorBinding
import com.ashique.qrscanner.helper.BitmapHelper.saveBitmap
import com.ashique.qrscanner.helper.ColorPicker.QrColorType
import com.ashique.qrscanner.helper.QrHelper
import com.ashique.qrscanner.helper.QrHelper.scanQrCodeMl
import com.ashique.qrscanner.services.ViewPagerAdapter
import com.github.alexzhirkevich.customqrgenerator.QrErrorCorrectionLevel
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class QrGenerator : AppCompatActivity() {

    private val binding by lazy {
        QrGeneratorBinding.inflate(layoutInflater)
    }


    companion object {

        var currentColorType: QrColorType? = null

        var qrBackground: Drawable? = null
        var qrLogo: Drawable? = null

        private var defaultColor = Color.BLACK
        var ballColor = defaultColor
        var frameColor = defaultColor
        var darkColor = defaultColor

        var useSolidColor = false
        var useLinearGradient = false
        var useRadialGradient = false
        var useSweepGradient = true

        var gradientColor0 = Color.RED
        var gradientColor1 = Color.GREEN

        var logoPadding: Float = .2f
        var logoSize: Float = .25f
        var darkPixelRoundness: Float = 0.0f
        var ballRoundness: Float = .25f
        var frameRoundness: Float = .25f

        var selectedQrShape: QrShape = QrShape.Default

        var selectedDarkPixelShape: QrVectorPixelShape =
            QrVectorPixelShape.RoundCorners(darkPixelRoundness)

        var selectedFrameShape: QrVectorFrameShape = QrVectorFrameShape.RoundCorners(frameRoundness)

        var selectedEyeBallShape: QrVectorBallShape = QrVectorBallShape.RoundCorners(ballRoundness)

        var selectedGradientOrientation: QrVectorColor.LinearGradient.Orientation =
            QrVectorColor.LinearGradient
                .Orientation.LeftDiagonal


    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)



        with(binding) {
            editInput.setText(R.string.app_name)

            updateQrCode()

            //  editInput.addTextChangedListener { updateQrCode() }

            editInput.addTextChangedListener(object : TextWatcher {
                private var job: Job? = null

                override fun afterTextChanged(s: Editable?) {
                    job?.cancel() // Cancel previous job
                    job = lifecycleScope.launch(Dispatchers.Default) {
                        delay(100) // Delay to allow user to finish typing
                        updateQrCode()
                    }
                }

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })



            viewPagerShapes.adapter = ViewPagerAdapter(this@QrGenerator)



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
                    saveBitmap(bitmap, Bitmap.CompressFormat.JPEG, "qr_code.png")
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
            padding = .100f
            fourthEyeEnabled = false
            errorCorrectionLevel = QrErrorCorrectionLevel.High


            background {
                drawable = qrBackground
                color = QrVectorColor.Solid(Color.WHITE)
                codeShape = selectedQrShape

            }
            logo {
                drawable = qrLogo
                size = logoSize
                padding = QrVectorLogoPadding.Natural(logoPadding)
                shape = QrVectorLogoShape.Circle
            }

            colors {

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
                darkPixel = selectedDarkPixelShape
                ball = selectedEyeBallShape
                frame = selectedFrameShape
            }
        }
    }


    fun updateQrCode() {
        lifecycleScope.launch(Dispatchers.IO) {
            // Generate QR code bitmap in a background thread
            val options = createQrOptions()
            val bitmap =
                QrCodeDrawable({ binding.editInput.text.toString() }, options).toBitmap(400, 400)

            // Switch to Main thread to update UI
            withContext(Dispatchers.Main) {
                binding.ivQrcode.setImageBitmap(bitmap)
            }

            // Perform QR code scanning in the background
            val qr = scanQrCodeMl(bitmap)

            withContext(Dispatchers.Main) {
                with(binding) {
                    if (qr.isNullOrEmpty()) {
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
                }
            }
        }


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