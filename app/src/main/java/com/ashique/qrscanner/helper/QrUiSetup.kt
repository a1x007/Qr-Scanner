package com.ashique.qrscanner.helper


import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.BitmapDrawable
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.ashique.qrscanner.R
import com.ashique.qrscanner.activity.QrGenerator
import com.ashique.qrscanner.activity.QrGenerator.Companion.ballColor
import com.ashique.qrscanner.activity.QrGenerator.Companion.ballRoundness
import com.ashique.qrscanner.activity.QrGenerator.Companion.centerCrop
import com.ashique.qrscanner.activity.QrGenerator.Companion.currentColorType
import com.ashique.qrscanner.activity.QrGenerator.Companion.darkColor
import com.ashique.qrscanner.activity.QrGenerator.Companion.darkPixelRoundness
import com.ashique.qrscanner.activity.QrGenerator.Companion.drawableBgColor
import com.ashique.qrscanner.activity.QrGenerator.Companion.frameColor
import com.ashique.qrscanner.activity.QrGenerator.Companion.frameRoundness
import com.ashique.qrscanner.activity.QrGenerator.Companion.gradientColor0
import com.ashique.qrscanner.activity.QrGenerator.Companion.gradientColor1
import com.ashique.qrscanner.activity.QrGenerator.Companion.logoPadding
import com.ashique.qrscanner.activity.QrGenerator.Companion.logoSize
import com.ashique.qrscanner.activity.QrGenerator.Companion.qrBackground
import com.ashique.qrscanner.activity.QrGenerator.Companion.qrPadding
import com.ashique.qrscanner.activity.QrGenerator.Companion.selectedDarkPixelShape
import com.ashique.qrscanner.activity.QrGenerator.Companion.selectedEyeBallShape
import com.ashique.qrscanner.activity.QrGenerator.Companion.selectedFrameShape
import com.ashique.qrscanner.activity.QrGenerator.Companion.selectedGradientOrientation
import com.ashique.qrscanner.activity.QrGenerator.Companion.selectedQrShape
import com.ashique.qrscanner.activity.QrGenerator.Companion.useLinearGradient
import com.ashique.qrscanner.activity.QrGenerator.Companion.useRadialGradient
import com.ashique.qrscanner.activity.QrGenerator.Companion.useSolidColor
import com.ashique.qrscanner.activity.QrGenerator.Companion.useSweepGradient
import com.ashique.qrscanner.colorpicker.ColorPickerDialog
import com.ashique.qrscanner.databinding.LayoutQrBackgroundBinding
import com.ashique.qrscanner.databinding.LayoutQrColorBinding
import com.ashique.qrscanner.databinding.LayoutQrLogoBinding
import com.ashique.qrscanner.databinding.LayoutQrSaveBinding
import com.ashique.qrscanner.databinding.LayoutQrShapeBinding
import com.ashique.qrscanner.databinding.LayoutQrTextBinding
import com.ashique.qrscanner.helper.Extensions.animateLayout
import com.ashique.qrscanner.helper.Extensions.createSeekBarListener
import com.github.alexzhirkevich.customqrgenerator.style.QrShape
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorBallShape
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorColor
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorFrameShape
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorPixelShape

object QrUiSetup {

    enum class QrColorType {
        QR, BALL, FRAME, DARK, COLOR0, COLOR1
    }


    private var darkPixelCircleSize: Float = 0.1f
    private var frameCircleSize: Float = 0.40f
    private var eyeCircleMiniSize: Float = 0.40f
    private var eyeCircleSize: Float = 0.90f
    private var setColorType: QrColorType = QrColorType.QR

    fun shapeSetting(binding: LayoutQrShapeBinding, onUpdate: () -> Unit) {

        with(binding) {
            binding.radioGroupQrShape.setOnCheckedChangeListener { _, checkedId ->
                selectedQrShape = when (checkedId) {
                    R.id.qrDefault -> QrShape.Default
                    R.id.qrCircle -> QrShape.Circle()
                    else -> QrShape.Default
                }
                onUpdate()
            }

            binding.radioGroupDarkPixelShape.setOnCheckedChangeListener { _, checkedId ->
                selectedDarkPixelShape = when (checkedId) {
                    R.id.pixelDefault -> QrVectorPixelShape.Default
                    R.id.pixelCircle -> QrVectorPixelShape.Circle(darkPixelCircleSize)
                    R.id.pixelRoundCorners25 -> QrVectorPixelShape.RoundCorners(0.25f)
                    R.id.pixelStar -> QrVectorPixelShape.Star
                    R.id.pixelRhombus -> QrVectorPixelShape.Rhombus()
                    R.id.pixelRoundCornersHorizontal -> QrVectorPixelShape.RoundCornersHorizontal()
                    R.id.radioButtonRoundCornersVertical -> QrVectorPixelShape.RoundCornersVertical()
                    else -> QrVectorPixelShape.Default // Default fallback
                }
                onUpdate()

                pixelCircleSizeSlider.visibility = if (checkedId == R.id.pixelCircle)
                    View.VISIBLE
                else View.GONE
            }

            binding.radioGroupFrameShape.setOnCheckedChangeListener { _, checkedId ->
                selectedFrameShape = when (checkedId) {
                    R.id.frameDefault -> QrVectorFrameShape.Default
                    R.id.frameCircle -> QrVectorFrameShape.Circle()
                    R.id.frameRoundCorners25 -> QrVectorFrameShape.RoundCorners(0.25f)
                    R.id.frameStar -> QrVectorFrameShape.AsPixelShape(QrVectorPixelShape.Star)
                    R.id.frameRhombus -> QrVectorFrameShape.AsPixelShape(QrVectorPixelShape.Rhombus())
                    R.id.frameCircleMini -> QrVectorFrameShape.AsPixelShape(
                        QrVectorPixelShape.Circle(
                            frameCircleSize
                        )
                    )

                    else -> QrVectorFrameShape.Default// Default fallback
                }
                onUpdate()

                frameCircleSizeSlider.visibility = if (checkedId == R.id.frameCircleMini)
                    View.VISIBLE
                else View.GONE

            }

            binding.radioGroupEyeShape.setOnCheckedChangeListener { _, checkedId ->
                selectedEyeBallShape = when (checkedId) {
                    R.id.eyeDefault -> QrVectorBallShape.Default
                    R.id.eyeCircle -> QrVectorBallShape.Circle(eyeCircleSize)
                    R.id.eyeRoundCorners25 -> QrVectorBallShape.RoundCorners(0.25f)
                    R.id.eyeStar -> QrVectorBallShape.AsPixelShape(QrVectorPixelShape.Star)
                    R.id.eyeRhombus -> QrVectorBallShape.AsPixelShape(QrVectorPixelShape.Rhombus())
                    R.id.eyeCircleMini -> QrVectorBallShape.AsPixelShape(
                        QrVectorPixelShape.Circle(
                            eyeCircleMiniSize
                        )
                    )

                    else -> QrVectorBallShape.Default// Default fallback
                }

                onUpdate()
                // Hide the slider when other shapes are selected
                binding.eyeCircleSizeSlider.visibility = if (checkedId == R.id.eyeCircleMini || checkedId == R.id.eyeCircle)
                    View.VISIBLE
                else View.GONE
            }

            pixelCircleSizeSlider.setOnSeekBarChangeListener(createSeekBarListener(onProgressChanged =  { progress ->
                // Update padding in the context of the activity
                darkPixelCircleSize = progress / 100f
                pixelCircleSizeText.text = progress.toString()
                selectedDarkPixelShape = QrVectorPixelShape.Circle(darkPixelCircleSize)
                onUpdate()
            }))

            frameCircleSizeSlider.setOnSeekBarChangeListener(createSeekBarListener(onProgressChanged =  { progress ->
                // Update padding in the context of the activity
                frameCircleSize = progress / 100f
                frameCircleSizeText.text = progress.toString()
                selectedFrameShape =
                    QrVectorFrameShape.AsPixelShape(QrVectorPixelShape.Circle(frameCircleSize))
                onUpdate()
            }))

            eyeCircleSizeSlider.setOnSeekBarChangeListener(createSeekBarListener(onProgressChanged =  { progress ->
                // Update padding in the context of the activity
                if (radioGroupEyeShape.checkedRadioButtonId == R.id.eyeCircle) {
                    eyeCircleSize = progress / 100f
                    selectedEyeBallShape = QrVectorBallShape.Circle(eyeCircleSize)
                } else {
                    eyeCircleMiniSize = progress / 100f
                    selectedEyeBallShape =
                        QrVectorBallShape.AsPixelShape(QrVectorPixelShape.Circle(eyeCircleMiniSize))
                }

                eyeCircleSizeText.text = progress.toString()

                onUpdate()
            }))

        }
    }


    fun logoSetting(binding: LayoutQrLogoBinding, onUpdate: () -> Unit) {
        val activity = binding.root.context as? QrGenerator


        with(binding) {

            btnImportLogo.setOnCheckedListener { isChecked ->
                if (isChecked) {
                    activity?.importDrawable(true, btnImportLogo)
                }
            }

            // Sliders
            paddingSlider.setOnSeekBarChangeListener(createSeekBarListener(onProgressChanged =  { progress ->
                // Update padding in the context of the activity
                logoPadding = progress / 100f
                onUpdate()
            }))

            logoSizeSlider.setOnSeekBarChangeListener(createSeekBarListener(onProgressChanged =  { progress ->
                logoSize = progress / 100f
                onUpdate()
            }))

            darkPixelRoundnessSlider.setOnSeekBarChangeListener(createSeekBarListener(onProgressChanged =  { progress ->
                darkPixelRoundness = progress / 100f
                selectedDarkPixelShape = QrVectorPixelShape.RoundCorners(darkPixelRoundness)

                onUpdate()
            }))

            ballRoundnessSlider.setOnSeekBarChangeListener(createSeekBarListener(onProgressChanged =  { progress ->
                ballRoundness = progress / 100f
                QrVectorBallShape.RoundCorners(ballRoundness)
                onUpdate()
            }))

            frameRoundnessSlider.setOnSeekBarChangeListener(createSeekBarListener(onProgressChanged =  { progress ->
                frameRoundness = progress / 100f
                QrVectorFrameShape.RoundCorners(frameRoundness)
                onUpdate()
            }))
        }
    }


    fun qrColorSetting(binding: LayoutQrColorBinding, onUpdate: () -> Unit) {
        // Color pickers
        with(binding) {
            val activity = binding.root.context as? AppCompatActivity

            val colorPickerDialog = ColorPickerDialog(ui = binding).apply {
                onColorChanged = { newColor ->

                    if (useSolidColor) {
                        when (currentColorType) {
                            QrColorType.BALL -> ballColor = newColor
                            QrColorType.FRAME -> frameColor = newColor
                            QrColorType.DARK -> darkColor = newColor
                            QrColorType.QR -> {
                                darkColor = newColor
                                frameColor = newColor
                                ballColor = newColor
                            }

                            else -> Unit
                        }
                    } else if (useLinearGradient) {
                        when (currentColorType) {
                            QrColorType.COLOR0 -> gradientColor0 = newColor
                            QrColorType.COLOR1 -> gradientColor1 = newColor
                            else -> Unit
                        }

                        QrVectorColor.LinearGradient(
                            colors = listOf(
                                0f to gradientColor0,
                                1f to gradientColor1,
                            ),
                            orientation = selectedGradientOrientation
                        )
                    }
                    onUpdate()

                }
            }


            radioGroupColor.setOnCheckedChangeListener { _, checkedId ->
                when (checkedId) {
                    R.id.qr_color -> {
                        // currentColorType = QrColorType.QR
                        setColorType = QrColorType.QR


                    }

                    R.id.qr_darkpixel_color -> {
                        //  currentColorType = QrColorType.DARK
                        setColorType = QrColorType.DARK


                    }

                    R.id.qr_eye_color -> {
                        // currentColorType = QrColorType.BALL
                        setColorType = QrColorType.BALL

                    }

                    R.id.qr_frame_color -> {
                        // currentColorType = QrColorType.FRAME
                        setColorType = QrColorType.FRAME

                    }
                }
            }

            radioGroupGradient.setOnCheckedChangeListener { _, checkedId ->
                selectedGradientOrientation = when (checkedId) {
                    R.id.gradient_bottom -> QrVectorColor.LinearGradient.Orientation.Vertical
                    R.id.gradient_right -> QrVectorColor.LinearGradient.Orientation.Horizontal
                    R.id.gradient_bottom_right -> QrVectorColor.LinearGradient.Orientation.LeftDiagonal
                    R.id.gradient_top_right -> QrVectorColor.LinearGradient.Orientation.RightDiagonal
                    else -> QrVectorColor.LinearGradient.Orientation.Vertical
                }

                when (checkedId) {
                    R.id.gradient_bottom, R.id.gradient_right, R.id.gradient_bottom_right, R.id.gradient_top_right -> {
                        setGradientFlags(
                            solid = false,
                            linear = true,
                            radial = false,
                            sweep = false
                        )
                    }

                    R.id.radial -> {
                        setGradientFlags(
                            solid = false,
                            linear = false,
                            radial = true,
                            sweep = false
                        )
                    }

                    R.id.sweep -> {
                        setGradientFlags(
                            solid = false,
                            linear = false,
                            radial = false,
                            sweep = true
                        )
                    }

                    else -> {
                        setGradientFlags(
                            solid = false,
                            linear = true,
                            radial = false,
                            sweep = false
                        )
                    }
                }
                onUpdate()
            }


            // show hide gradientFrame with animation
            root.animateLayout()


            if (btnSolidColor.isChecked) {
                setGradientFlags(solid = true, linear = false, radial = false, sweep = false)
            }

            btnSolidColor.setOnCheckedListener { isCheck ->
                if (isCheck) {
                    btnGradientColor.isChecked = false
                    setGradientFlags(solid = true, linear = false, radial = false, sweep = false)
                    onUpdate()
                }
            }

            btnGradientColor.setOnCheckedListener { isCheck ->

                if (isCheck) {
                    gradientFrame.isVisible = true
                    btnSolidColor.isChecked = false
                    setGradientFlags(solid = false, linear = true, radial = false, sweep = false)
                    onUpdate()
                } else {
                    gradientFrame.isVisible = false
                }
            }

            btnGradientColor0.setOnCheckedListener { isCheck ->

                if (isCheck) {
                    btnGradientColor1.isChecked = false
                    currentColorType =
                        if (btnGradientColor.isChecked) QrColorType.COLOR0 else setColorType
                    activity?.supportFragmentManager?.let {
                        colorPickerDialog.show(it, "ColorPickerDialog")
                    }
                    onUpdate()
                }

            }


            btnGradientColor1.setOnCheckedListener { isCheck ->
                if (isCheck) {
                    btnGradientColor0.isChecked = false
                    currentColorType = QrColorType.COLOR1
                    activity?.supportFragmentManager?.let {
                        colorPickerDialog.show(it, "ColorPickerDialog")
                    }
                }

            }
        }
    }




    private fun setGradientFlags(solid: Boolean, linear: Boolean, radial: Boolean, sweep: Boolean) {
        useSolidColor = solid
        useLinearGradient = linear

        useRadialGradient = radial
        useSweepGradient = sweep
    }

    fun textSetting(binding: LayoutQrTextBinding, function: () -> Unit?) {

    }


    fun backgroundSetting(binding: LayoutQrBackgroundBinding, onUpdate: () -> Unit?) {
        val activity = binding.root.context as? QrGenerator

        with(binding) {

            btnImportBackground.setOnCheckedListener { isChecked ->
                if (isChecked) {
                    activity?.importDrawable(false, btnImportBackground)
                }
            }

            val colorPickerDialog = ColorPickerDialog(ui2 = binding).apply {
                onColorChanged = { newColor ->
                    drawableBgColor = newColor
                    qrBackground?.let { drawable ->
                        // Ensure the drawable is a BitmapDrawable
                        val bitmap = drawable.bitmap
                        val paint = Paint().apply {
                            colorFilter = PorterDuffColorFilter(newColor, PorterDuff.Mode.SRC_ATOP)
                        }
                        val newBitmap =
                            bitmap.config?.let {
                                Bitmap.createBitmap(
                                    bitmap.width, bitmap.height,
                                    it
                                )
                            }
                        val canvas = newBitmap?.let { Canvas(it) }
                        canvas?.drawBitmap(bitmap, 0f, 0f, paint)
                        qrBackground = BitmapDrawable(resources, newBitmap)
                        onUpdate()
                    }
                }
            }

            btnColor.setOnCheckedListener { isChecked ->
                if (isChecked) {
                    activity?.supportFragmentManager?.let {
                        colorPickerDialog.show(it, "ColorPickerDialog")
                    }
                    onUpdate()
                }
            }

            btnCenterCrop.setOnCheckedListener { isChecked ->
                centerCrop = isChecked
                onUpdate()
            }


            paddingSlider.apply {
                max = 50 // Set the maximum value to represent 0.5f
                progress = (qrPadding * 100).toInt() // Initialize the progress based on qrPadding
                setOnSeekBarChangeListener(createSeekBarListener(onProgressChanged =  { progress ->
                    qrPadding = progress / 100f // Map progress to the range 0.0f to 0.5f
                    onUpdate()
                }))
            }
        }
    }


    fun saveSetting(binding: LayoutQrSaveBinding, function: () -> Unit) {

    }


}
