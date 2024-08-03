package com.ashique.qrscanner.activity

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toBitmap
import androidx.core.widget.addTextChangedListener
import com.ashique.qrscanner.R
import com.ashique.qrscanner.databinding.QrGeneratorBinding
import com.ashique.qrscanner.helper.BitmapHelper.saveBitmap
import com.ashique.qrscanner.helper.ColorPicker.QrColorType
import com.ashique.qrscanner.helper.QrUiSetup
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

class QrGenerator : AppCompatActivity(){

    private val binding by lazy {
        QrGeneratorBinding.inflate(layoutInflater)
    }



    companion object {

        var currentColorType: QrColorType? = null
        var qrBackground = null
        var qrLogo = null

        var defaultColor = Color.BLACK
        var ballColor = defaultColor
        var frameColor = defaultColor
        var darkColor = defaultColor

        var logoPadding: Float = 0.1f
        var logoSize: Float = 0.25f
        var darkPixelRoundness: Float = 0.1f
        var ballRoundness: Float = 0.25f
        var frameRoundness: Float = 0.25f

        var selectedDarkPixelShape: QrVectorPixelShape =
            QrVectorPixelShape.RoundCorners(darkPixelRoundness)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)



        with(binding) {
            editInput.setText(R.string.app_name)

            updateQrCode()

            editInput.addTextChangedListener {
                updateQrCode()
            }

            viewPagerShapes.adapter = ViewPagerAdapter(this@QrGenerator)




            btnSaveQr.setOnClickListener {
                binding.ivQrcode.drawable?.let { drawable ->
                    val bitmap = drawable.toBitmap(binding.ivQrcode.width, binding.ivQrcode.height)
                    saveBitmap(bitmap, Bitmap.CompressFormat.JPEG, "qr_code.png")
                } ?: run {
                    Toast.makeText(
                        this@QrGenerator,
                        "QR code image is not available.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        // Initialize UI
        QrUiSetup.setupUi(binding) {
            updateQrCode()
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
                codeShape = QrShape.Circle()
            }
            logo {
                drawable = qrLogo
                size = logoSize
                padding = QrVectorLogoPadding.Natural(logoPadding)
                shape = QrVectorLogoShape.Circle
            }
            colors {
                dark = QrVectorColor.Solid(darkColor)
                ball = QrVectorColor.Solid(ballColor)
                frame = QrVectorColor.Solid(frameColor)
            }
            shapes {
                darkPixel = selectedDarkPixelShape
                ball = QrVectorBallShape.RoundCorners(ballRoundness)
                frame = QrVectorFrameShape.RoundCorners(frameRoundness)


                QrVectorFrameShape.AsDarkPixels
                QrVectorFrameShape.Circle(.2f)
                QrVectorFrameShape.AsPixelShape(QrVectorPixelShape.Circle())
                QrVectorPixelShape.Default
                QrVectorPixelShape.Circle()
                QrVectorPixelShape.RoundCorners(.25f)
                QrVectorPixelShape.Star
                QrVectorPixelShape.Rhombus()
                QrVectorPixelShape.RoundCornersHorizontal()
                QrVectorPixelShape.RoundCornersVertical()
            }
        }
    }


    fun updateQrCode() {
        val options = createQrOptions()
        binding.ivQrcode.setImageBitmap(
            QrCodeDrawable({ binding.editInput.text.toString() }, options).toBitmap(1024, 1024)
        )
    }


}
