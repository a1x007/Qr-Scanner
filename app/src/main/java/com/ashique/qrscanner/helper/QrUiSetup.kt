package com.ashique.qrscanner.helper


import android.app.Activity
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import com.ashique.qrscanner.R
import com.ashique.qrscanner.activity.QrGenerator
import com.ashique.qrscanner.activity.QrGenerator.Companion.backgroundHeight
import com.ashique.qrscanner.activity.QrGenerator.Companion.backgroundWidth
import com.ashique.qrscanner.activity.QrGenerator.Companion.ballColor
import com.ashique.qrscanner.activity.QrGenerator.Companion.ballRoundness
import com.ashique.qrscanner.activity.QrGenerator.Companion.brightness
import com.ashique.qrscanner.activity.QrGenerator.Companion.centerCrop
import com.ashique.qrscanner.activity.QrGenerator.Companion.colorize
import com.ashique.qrscanner.activity.QrGenerator.Companion.contrast
import com.ashique.qrscanner.activity.QrGenerator.Companion.currentColorType
import com.ashique.qrscanner.activity.QrGenerator.Companion.darkColor
import com.ashique.qrscanner.activity.QrGenerator.Companion.darkPixelRoundness
import com.ashique.qrscanner.activity.QrGenerator.Companion.dotSize
import com.ashique.qrscanner.activity.QrGenerator.Companion.frameColor
import com.ashique.qrscanner.activity.QrGenerator.Companion.frameRoundness
import com.ashique.qrscanner.activity.QrGenerator.Companion.gradientColor0
import com.ashique.qrscanner.activity.QrGenerator.Companion.gradientColor1
import com.ashique.qrscanner.activity.QrGenerator.Companion.logoPadding
import com.ashique.qrscanner.activity.QrGenerator.Companion.logoSize
import com.ashique.qrscanner.activity.QrGenerator.Companion.originalBackground
import com.ashique.qrscanner.activity.QrGenerator.Companion.qrBackgroundColor
import com.ashique.qrscanner.activity.QrGenerator.Companion.qrPadding
import com.ashique.qrscanner.activity.QrGenerator.Companion.selectedDarkPixelShape
import com.ashique.qrscanner.activity.QrGenerator.Companion.selectedEyeBallShape
import com.ashique.qrscanner.activity.QrGenerator.Companion.selectedFrameShape
import com.ashique.qrscanner.activity.QrGenerator.Companion.selectedGradientOrientation
import com.ashique.qrscanner.activity.QrGenerator.Companion.selectedQrShape
import com.ashique.qrscanner.activity.QrGenerator.Companion.stillBackground
import com.ashique.qrscanner.activity.QrGenerator.Companion.useBinary
import com.ashique.qrscanner.activity.QrGenerator.Companion.useHalftone
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
import com.ashique.qrscanner.helper.Extensions.dpToPx
import com.ashique.qrscanner.helper.ImageEnhancer.convert
import com.github.alexzhirkevich.customqrgenerator.style.QrShape
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorBallShape
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorColor
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorFrameShape
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorPixelShape

object QrUiSetup {

    enum class QrColorType {
        QR, BALL, FRAME, DARK, COLOR0, COLOR1
    }

    enum class QrFormats {
        JPG, PNG, WEBP, GIF
    }

    private var darkPixelBubbleMaxSize = .6f
    private var darkPixelBubbleSize = .3f
    private var darkPixelSquareSize = 1f
    private var darkPixelCircleSize = 0.1f
    private var frameCircleSize = 0.40f
    private var eyeCircleMiniSize = 0.40f
    private var eyeCircleSize = 0.90f
    private var setColorType: QrColorType = QrColorType.QR


    fun shapeSetting(ui: LayoutQrShapeBinding, onUpdate: () -> Unit) {

        with(ui) {
            val lifecycleOwner = ui.root.context as? LifecycleOwner

            rootLayout.animateLayout()

            groupQrShape.setOnCheckedChangeListener { _, checkedId ->
                selectedQrShape = when (checkedId) {
                    R.id.qrDefault -> QrShape.Default
                    R.id.qrCircle -> QrShape.Circle()
                    else -> QrShape.Default
                }
                onUpdate()
            }

            groupDarkPixelShape.setOnCheckedChangeListener { _, checkedId ->
                // Map checkedId to its corresponding pixel shape and value
                val (shape, progressValue) = when (checkedId) {
                    R.id.pixelDefault -> QrVectorPixelShape.Square(darkPixelSquareSize) to darkPixelSquareSize
                    R.id.pixelCircle -> QrVectorPixelShape.Circle(darkPixelCircleSize) to darkPixelCircleSize
                    R.id.pixelRoundCorners -> QrVectorPixelShape.RoundCorners(darkPixelRoundness) to darkPixelRoundness
                    R.id.pixelBubble -> QrVectorPixelShape.Bubble(
                        darkPixelBubbleSize, darkPixelBubbleMaxSize
                    ) to darkPixelBubbleSize

                    R.id.pixelRhombus -> QrVectorPixelShape.Rhombus() to null
                    R.id.pixelRoundCornersHorizontal -> QrVectorPixelShape.RoundCornersHorizontal() to null
                    R.id.radioButtonRoundCornersVertical -> QrVectorPixelShape.RoundCornersVertical() to null
                    else -> QrVectorPixelShape.Default to null
                }

                // Update selectedDarkPixelShape and UI elements
                selectedDarkPixelShape = shape
                onUpdate()

                // Set visibility of pixelSliderFrame based on if the shape has a progress value
                pixelSliderFrame.visibility = if (progressValue != null) View.VISIBLE else View.GONE

                // Update SeekBar progress if applicable
                pixelSlider.progress =
                    (progressValue?.let { it * 100 } ?: pixelSlider.progress).toInt()
            }



            groupFrameShape.setOnCheckedChangeListener { _, checkedId ->
                selectedFrameShape = when (checkedId) {
                    R.id.frameDefault -> QrVectorFrameShape.Default
                    R.id.frameCircle -> QrVectorFrameShape.Circle()
                    R.id.frameRoundCorners25 -> QrVectorFrameShape.RoundCorners(0.25f)
                    R.id.frameStar -> QrVectorFrameShape.AsPixelShape(
                        QrVectorPixelShape.Bubble(
                            .3f, .6f
                        )
                    )

                    R.id.frameRhombus -> QrVectorFrameShape.AsPixelShape(QrVectorPixelShape.Rhombus())
                    R.id.frameCircleMini -> QrVectorFrameShape.AsPixelShape(
                        QrVectorPixelShape.Circle(
                            frameCircleSize
                        )
                    )

                    else -> QrVectorFrameShape.Default// Default fallback
                }
                onUpdate()

                frameCircleSizeSlider.visibility =
                    if (checkedId == R.id.frameCircleMini) View.VISIBLE
                    else View.GONE

            }


            ui.radioGroupEyeShape.setOnCheckedChangeListener { _, checkedId ->
                selectedEyeBallShape = when (checkedId) {
                    R.id.eyeDefault -> QrVectorBallShape.Default
                    R.id.eyeCircle -> QrVectorBallShape.Circle(eyeCircleSize)
                    R.id.eyeRoundCorners25 -> QrVectorBallShape.RoundCorners(0.25f)
                    R.id.eyeStar -> QrVectorBallShape.AsPixelShape(
                        QrVectorPixelShape.Bubble(
                            .3f, .6f
                        )
                    )

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
                ui.eyeCircleSizeSlider.visibility =
                    if (checkedId == R.id.eyeCircleMini || checkedId == R.id.eyeCircle) View.VISIBLE
                    else View.GONE
            }


            pixelSlider.setOnSeekBarChangeListener(createSeekBarListener(onProgressChanged = { progress ->
                val size = progress / 100f
                val (shape, updatedSize) = when (groupDarkPixelShape.checkedRadioButtonId) {
                    R.id.pixelDefault -> {
                        darkPixelSquareSize = size
                        QrVectorPixelShape.Square(size) to size
                    }

                    R.id.pixelRoundCorners -> {
                        darkPixelRoundness = size
                        QrVectorPixelShape.RoundCorners(size) to size
                    }

                    R.id.pixelBubble -> {
                        darkPixelBubbleSize = size.coerceIn(0f, 0.7f)
                        darkPixelBubbleMaxSize = (size + 0.2f).coerceIn(0f, 1f)
                        QrVectorPixelShape.Bubble(
                            darkPixelBubbleSize,
                            darkPixelBubbleMaxSize
                        ) to size
                    }

                    else -> {
                        darkPixelCircleSize = size
                        QrVectorPixelShape.Circle(size) to size
                    }
                }

                selectedDarkPixelShape = shape
                pixelSizeText.text = updatedSize.toString()
                onUpdate()
            }))



            frameCircleSizeSlider.setOnSeekBarChangeListener(createSeekBarListener(onProgressChanged = { progress ->
                // Update padding in the context of the activity
                frameCircleSize = progress / 100f
                frameCircleSizeText.text = progress.toString()
                selectedFrameShape =
                    QrVectorFrameShape.AsPixelShape(QrVectorPixelShape.Circle(frameCircleSize))
                onUpdate()
            }))

            eyeCircleSizeSlider.setOnSeekBarChangeListener(createSeekBarListener(onProgressChanged = { progress ->
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
            paddingSlider.setOnSeekBarChangeListener(createSeekBarListener(onProgressChanged = { progress ->
                // Update padding in the context of the activity
                logoPadding = progress / 100f
                onUpdate()
            }))

            logoSizeSlider.setOnSeekBarChangeListener(createSeekBarListener(onProgressChanged = { progress ->
                logoSize = progress / 100f
                onUpdate()
            }))

            darkPixelRoundnessSlider.setOnSeekBarChangeListener(
                createSeekBarListener(onProgressChanged = { progress ->
                    darkPixelRoundness = progress / 100f
                    selectedDarkPixelShape = QrVectorPixelShape.RoundCorners(darkPixelRoundness)

                    onUpdate()
                })
            )

            ballRoundnessSlider.setOnSeekBarChangeListener(createSeekBarListener(onProgressChanged = { progress ->
                ballRoundness = progress / 100f
                QrVectorBallShape.RoundCorners(ballRoundness)
                onUpdate()
            }))

            frameRoundnessSlider.setOnSeekBarChangeListener(createSeekBarListener(onProgressChanged = { progress ->
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
            root.animateLayout()

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
                            ), orientation = selectedGradientOrientation
                        )
                    }
                    onUpdate()

                }
            }


            radioGroupColor.setOnCheckedChangeListener { _, checkedId ->
                when (checkedId) {
                    R.id.qr_color -> {
                        setColorType = QrColorType.QR
                        colorTypeFrame.isVisible = true
                    }

                    R.id.qr_darkpixel_color -> {
                        setColorType = QrColorType.DARK
                        colorTypeFrame.isVisible = true

                    }

                    R.id.qr_eye_color -> {
                        setColorType = QrColorType.BALL
                        colorTypeFrame.isVisible = true
                    }

                    R.id.qr_frame_color -> {
                        setColorType = QrColorType.FRAME
                        colorTypeFrame.isVisible = true

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
                            solid = false, linear = true, radial = false, sweep = false
                        )
                    }

                    R.id.radial -> {
                        setGradientFlags(
                            solid = false, linear = false, radial = true, sweep = false
                        )
                    }

                    R.id.sweep -> {
                        setGradientFlags(
                            solid = false, linear = false, radial = false, sweep = true
                        )
                    }

                    else -> {
                        setGradientFlags(
                            solid = false, linear = true, radial = false, sweep = false
                        )
                    }
                }
                onUpdate()
            }




            if (btnSolidColor.isChecked) {
                setGradientFlags(solid = true, linear = false, radial = false, sweep = false)
            }

            btnSolidColor.setOnCheckedListener { isCheck ->
                if (isCheck) {
                    btnGradientColor.isChecked = false
                    setGradientFlags(solid = true, linear = false, radial = false, sweep = false)
                    onUpdate()
                    colorChangerFrame.isVisible = true
                } else {
                    colorChangerFrame.isVisible = false
                }
            }

            btnGradientColor.setOnCheckedListener { isCheck ->

                if (isCheck) {
                    gradientFrame.isVisible = true
                    btnSolidColor.isChecked = false
                    btnGradientColor1.isVisible = true
                    colorChangerFrame.isVisible = true
                    setGradientFlags(solid = false, linear = true, radial = false, sweep = false)
                    onUpdate()
                } else {
                    gradientFrame.isVisible = false
                    btnGradientColor1.isVisible = false
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

            rootLayout.animateLayout()

            btnImportBackground.setOnCheckedListener { isChecked ->
                if (isChecked) {
                    activity?.importDrawable(false, btnImportBackground, binding)
                    thresholdLayout.isVisible = false
                    radioGroupEffect.clearCheck()


                }

                // Log.d("Debug", "stillBackground: $stillBackground")
                //  Log.d("Debug", "gifBackground: $gifBackground")

                // effectLayout.isVisible = (stillBackground != null || gifBackground != null)
            }




            btnAdjust.setOnCheckedListener { isChecked ->
                adjustLayout.isVisible = isChecked
            }


            val colorPickerDialog = ColorPickerDialog(ui2 = binding).apply {
                onColorChanged = { newColor ->

                    qrBackgroundColor = newColor


                    onUpdate()

                    /*   stillBackground?.let { drawable ->
                           // Ensure the drawable is a BitmapDrawable
                           val bitmap = drawable.bitmap
                           val paint = Paint().apply {
                               colorFilter = PorterDuffColorFilter(newColor, PorterDuff.Mode.SRC_ATOP)
                           }
                           val newBitmap = bitmap.config?.let {
                               Bitmap.createBitmap(
                                   bitmap.width, bitmap.height, it
                               )
                           }
                           val canvas = newBitmap?.let { Canvas(it) }
                           canvas?.drawBitmap(bitmap, 0f, 0f, paint)
                           stillBackground = BitmapDrawable(resources, newBitmap)
                           onUpdate()
                       }*/
                }


            }

            radioGroupEffect.setOnCheckedChangeListener { _, checkedId ->
                when (checkedId) {
                    R.id.halftone_effect -> {
                        useHalftone = true
                        useBinary = false
                        thresholdLayout.isVisible = true
                        activity?.updateQrBackground()
                        onUpdate()
                    }

                    R.id.binary_effect -> {
                        useHalftone = false
                        useBinary = true
                        thresholdLayout.isVisible = true
                        activity?.updateQrBackground()
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

            btnColorMode.setOnCheckedListener { isChecked ->
                colorize = isChecked
                activity?.updateQrBackground()
                onUpdate()
            }


            btnEffect.setOnCheckedListener { isChecked ->
                effectLayout.isVisible = isChecked
            }


            thresholdSlider.apply {
                max = 10
                setOnSeekBarChangeListener(createSeekBarListener(onProgressChanged = { progress ->
                    // Ensure the progress is always within 1 to 10
                    val adjustedProgress = progress.coerceIn(1, 10)
                    if (dotSize != adjustedProgress) {
                        dotSize = adjustedProgress
                        dotSizeText.text = adjustedProgress.toString()

                        activity?.updateQrBackground()
                        onUpdate()
                    }
                }))
            }

            brightnessSlider.apply {
                setOnSeekBarChangeListener(createSeekBarListener(onProgressChanged = { progress ->
                    val adjustedProgress = if (progress in 99..101) 100 else progress

                    brightnessText.text = ((adjustedProgress - 100) / 100f).toString()
                    brightness = (adjustedProgress - 100) / 100f

                    activity?.updateQrBackground()
                    onUpdate()
                }))
            }



            contrastSlider.apply {
                setOnSeekBarChangeListener(createSeekBarListener(onProgressChanged = { progress ->
                    contrastText.text = (progress / 100f).toString()
                    contrast = progress / 100f

                    activity?.updateQrBackground()
                    onUpdate()

                }))
            }



            heightSeekBar.apply {
                setOnSeekBarChangeListener(createSeekBarListener(onProgressChanged = { progress ->
                    heightText.text = progress.toString()
                    backgroundHeight = activity?.resources?.let { progress.dpToPx(it) }
                    onUpdate()

                }))
            }

            widthSeekBar.apply {
                setOnSeekBarChangeListener(createSeekBarListener(onProgressChanged = { progress ->
                    widthText.text = progress.toString()
                    backgroundWidth = activity?.resources?.let { progress.dpToPx(it) }
                    onUpdate()

                }))
            }


            paddingSlider.apply {
                max = 50 // Set the maximum value to represent 0.5f
                progress = (qrPadding * 100).toInt() // Initialize the progress based on qrPadding
                setOnSeekBarChangeListener(createSeekBarListener(onProgressChanged = { progress ->
                    qrPadding = progress / 100f // Map progress to the range 0.0f to 0.5f
                    onUpdate()
                }))
            }
        }


    }

    private fun Activity.updateQrBackground() {
        originalBackground?.let { drawable ->
            val bitmap = convert(
                drawable.bitmap,
                brightness = brightness,
                contrast = contrast,
                halftone = useHalftone,
                binary = useBinary,
                colorized = colorize,
                dotSize = dotSize
            )
            stillBackground = resources?.let { bitmap.toDrawable(it) }
        }
    }

    fun saveSetting(binding: LayoutQrSaveBinding, onUpdate: () -> Unit) {
        with(binding) {
            val activity = binding.root.context as? QrGenerator

            rootLayout.animateLayout()

            radioGroupFormats.setOnCheckedChangeListener { _, checkedId ->
                when (checkedId) {
                    R.id.format_png -> {
                        activity?.saveQrCode(QrFormats.PNG, binding)
                    }

                    R.id.format_jpg -> {
                        activity?.saveQrCode(QrFormats.JPG, binding)
                    }

                    R.id.format_webp -> {
                        activity?.saveQrCode(QrFormats.WEBP, binding)
                    }


                    R.id.format_gif -> {
                        activity?.saveQrCode(QrFormats.GIF, binding)
                    }
                }
            }


        }
    }


}
