package com.ashique.qrscanner.helper


import android.util.Log
import android.view.View
import android.view.View.FOCUS_DOWN
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.RadioGroup
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import com.ashique.qrscanner.R
import com.ashique.qrscanner.activity.QrGenerator
import com.ashique.qrscanner.activity.QrGenerator.Companion.backgroundHeight
import com.ashique.qrscanner.activity.QrGenerator.Companion.backgroundWidth
import com.ashique.qrscanner.activity.QrGenerator.Companion.ballColor
import com.ashique.qrscanner.activity.QrGenerator.Companion.brightness
import com.ashique.qrscanner.activity.QrGenerator.Companion.centerCrop
import com.ashique.qrscanner.activity.QrGenerator.Companion.colorize
import com.ashique.qrscanner.activity.QrGenerator.Companion.contrast
import com.ashique.qrscanner.activity.QrGenerator.Companion.cornerEyesColor
import com.ashique.qrscanner.activity.QrGenerator.Companion.currentColorType
import com.ashique.qrscanner.activity.QrGenerator.Companion.darkColor
import com.ashique.qrscanner.activity.QrGenerator.Companion.darkPixelRoundness
import com.ashique.qrscanner.activity.QrGenerator.Companion.dotSize
import com.ashique.qrscanner.activity.QrGenerator.Companion.frameColor
import com.ashique.qrscanner.activity.QrGenerator.Companion.gifBackground
import com.ashique.qrscanner.activity.QrGenerator.Companion.gradientColor0
import com.ashique.qrscanner.activity.QrGenerator.Companion.gradientColor00
import com.ashique.qrscanner.activity.QrGenerator.Companion.gradientColor1
import com.ashique.qrscanner.activity.QrGenerator.Companion.gradientColor11
import com.ashique.qrscanner.activity.QrGenerator.Companion.lightColor
import com.ashique.qrscanner.activity.QrGenerator.Companion.logoPadding
import com.ashique.qrscanner.activity.QrGenerator.Companion.logoSize
import com.ashique.qrscanner.activity.QrGenerator.Companion.originalBackground
import com.ashique.qrscanner.activity.QrGenerator.Companion.qrBackgroundColor
import com.ashique.qrscanner.activity.QrGenerator.Companion.qrLogo
import com.ashique.qrscanner.activity.QrGenerator.Companion.qrPadding
import com.ashique.qrscanner.activity.QrGenerator.Companion.selectedDarkPixelShape
import com.ashique.qrscanner.activity.QrGenerator.Companion.selectedEyeBallShape
import com.ashique.qrscanner.activity.QrGenerator.Companion.selectedFrameShape
import com.ashique.qrscanner.activity.QrGenerator.Companion.selectedGradientOrientation
import com.ashique.qrscanner.activity.QrGenerator.Companion.selectedQrShape
import com.ashique.qrscanner.activity.QrGenerator.Companion.selectedTimingShape
import com.ashique.qrscanner.activity.QrGenerator.Companion.stillBackground
import com.ashique.qrscanner.activity.QrGenerator.Companion.timingLinesColor
import com.ashique.qrscanner.activity.QrGenerator.Companion.useBinary
import com.ashique.qrscanner.activity.QrGenerator.Companion.useGradientHighlight
import com.ashique.qrscanner.activity.QrGenerator.Companion.useHalftone
import com.ashique.qrscanner.activity.QrGenerator.Companion.useLinearGradient
import com.ashique.qrscanner.activity.QrGenerator.Companion.useRadialGradient
import com.ashique.qrscanner.activity.QrGenerator.Companion.useSolidColor
import com.ashique.qrscanner.activity.QrGenerator.Companion.useSweepGradient
import com.ashique.qrscanner.activity.QrGenerator.Companion.versionEyesColor
import com.ashique.qrscanner.colorpicker.ColorPickerDialog
import com.ashique.qrscanner.databinding.LayoutQrBackgroundBinding
import com.ashique.qrscanner.databinding.LayoutQrColorBinding
import com.ashique.qrscanner.databinding.LayoutQrLogoBinding
import com.ashique.qrscanner.databinding.LayoutQrSaveBinding
import com.ashique.qrscanner.databinding.LayoutQrShapeBinding
import com.ashique.qrscanner.helper.ImageEnhancer.convert
import com.ashique.qrscanner.utils.Extensions.animateLayout
import com.ashique.qrscanner.utils.Extensions.createSeekBarListener
import com.ashique.qrscanner.utils.Extensions.dpToPx
import com.github.alexzhirkevich.customqrgenerator.style.QrShape
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorBallShape
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorColor
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorFrameShape
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorPixelShape

object QrUiSetup {

    enum class DataType {
        TEXT, URL, CONTACT, SMS, PHONE, EMAIL, WIFI, GEO, BCARD, EVENT, YOUTUBE, GOOGLEPLAY, BKASH
    }

    enum class QrColorType {
        QR, BALL, FRAME, DARK, LIGHT, VERSION_EYES, CORNER_EYES, TIMING_LINE, COLOR0, COLOR1
    }

    enum class QrFormats {
        JPG, PNG, WEBP, GIF
    }


    private var darkPixelBubbleMaxSize = .6f
    private var darkPixelBubbleMedSize = .45f
    private var darkPixelBubbleSize = .3f
    private var darkPixelSquareSize = 1f
    private var darkPixelCircleSize = 0.1f
    private var frameRoundness = 0.25f
    private var frameCircleSize = 0.40f
    private var eyeCircleMiniSize = 0.40f
    private var eyeCircleSize = 0.90f

    private var planetDashedCircleSize = 3f
    private var frameBubbleMaxSize = .6f
    private var frameBubbleMedSize = .45f
    private var frameBubbleSize = .3f

    private var eyeRoundness = 0.25f

    private var timingSquareSize = .6f
    private var timingCircleSize = .45f
    private var timingBubbleMaxSize = .6f
    private var timingBubbleMedSize = .45f
    private var timingBubbleSize = .3f
    private var timingRoundness = 0.25f
    private var timingCircleMiniSize = 0.40f
    private var timingRhombusSize = 0.90f

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
                        darkPixelBubbleSize, darkPixelBubbleMedSize, darkPixelBubbleMaxSize,
                    ) to darkPixelBubbleSize

                    R.id.pixelRhombus -> QrVectorPixelShape.Box() to null
                    R.id.pixelHitech -> QrVectorPixelShape.Hitech(
                        .3f, .45f, .6f, .5f
                    ) to null

                    R.id.radioButtonRoundCornersVertical -> QrVectorPixelShape.Sticky(.5f) to null
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
                        darkPixelBubbleMedSize = (size + 0.1f).coerceIn(0f, 1f)
                        darkPixelBubbleMaxSize = (size + 0.2f).coerceIn(0f, 1f)
                        QrVectorPixelShape.Bubble(
                            darkPixelBubbleSize, darkPixelBubbleMedSize, darkPixelBubbleMaxSize
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



            groupFrameShape.setOnCheckedChangeListener { _, checkedId ->
                selectedFrameShape = when (checkedId) {
                    R.id.frameDefault -> QrVectorFrameShape.Default
                    R.id.frameCircle -> QrVectorFrameShape.Circle()
                    R.id.frameRoundCorners -> QrVectorFrameShape.RoundCorners(
                        frameRoundness, 1f, topLeft = false
                    )

                    R.id.frameBubble -> QrVectorFrameShape.AsPixelShape(
                        QrVectorPixelShape.Bubble(
                            .3f, .45f, .6f
                        )
                    )

                    R.id.framePlanet -> QrVectorFrameShape.Planet(width = planetDashedCircleSize)
                    R.id.frameCircleMini -> QrVectorFrameShape.AsPixelShape(
                        QrVectorPixelShape.Circle(
                            frameCircleSize
                        )
                    )

                    else -> QrVectorFrameShape.Default// Default fallback
                }
                onUpdate()

                frameSliderFrame.visibility =
                    if (checkedId == R.id.frameCircleMini || checkedId == R.id.frameRoundCorners || checkedId == R.id.framePlanet) View.VISIBLE
                    else View.GONE

            }


            frameSlider.setOnSeekBarChangeListener(createSeekBarListener(onProgressChanged = { progress ->
                // Update padding in the context of the activity
                val size = progress / 100f
                when (groupFrameShape.checkedRadioButtonId) {
                    R.id.frameCircleMini -> {
                        frameCircleSize = size

                        selectedFrameShape = QrVectorFrameShape.AsPixelShape(
                            QrVectorPixelShape.Circle(
                                frameCircleSize
                            )
                        )
                    }

                    R.id.frameRoundCorners -> {
                        frameRoundness = size
                        selectedFrameShape = QrVectorFrameShape.RoundCorners(frameRoundness)

                    }

                    R.id.framePlanet -> {
                        planetDashedCircleSize = size
                        selectedFrameShape =
                            QrVectorFrameShape.Planet(width = planetDashedCircleSize)
                    }

                    R.id.pixelBubble -> {
                        frameBubbleSize = size.coerceIn(0f, 0.7f)
                        frameBubbleMedSize = (size + 0.1f).coerceIn(0f, 1f)
                        frameBubbleMaxSize = (size + 0.2f).coerceIn(0f, 1f)
                        selectedFrameShape = QrVectorFrameShape.AsPixelShape(
                            QrVectorPixelShape.Bubble(
                                darkPixelBubbleSize, darkPixelBubbleMedSize, darkPixelBubbleMaxSize
                            )
                        )
                    }
                }

                frameSizeText.text = progress.toString()
                onUpdate()
            }))


            groupEyeShape.setOnCheckedChangeListener { _, checkedId ->
                selectedEyeBallShape = when (checkedId) {
                    R.id.eyeDefault -> QrVectorBallShape.Default
                    R.id.eyeCircle -> QrVectorBallShape.Circle(1f)
                    R.id.eyeRoundCorners -> QrVectorBallShape.RoundCorners(eyeRoundness)
                    R.id.eyeStar -> QrVectorBallShape.AsPixelShape(
                        QrVectorPixelShape.Bubble(
                            .3f, .45f, .6f
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
                eyeSliderFrame.visibility =
                    if (checkedId == R.id.eyeCircleMini || checkedId == R.id.eyeRoundCorners) View.VISIBLE
                    else View.GONE
            }

            eyeCircleSizeSlider.setOnSeekBarChangeListener(createSeekBarListener(onProgressChanged = { progress ->
                // Update padding in the context of the activity
                if (groupEyeShape.checkedRadioButtonId == R.id.eyeRoundCorners) {
                    eyeRoundness = progress / 100f
                    selectedEyeBallShape = QrVectorBallShape.RoundCorners(eyeRoundness)
                } else {
                    eyeCircleMiniSize = progress / 100f
                    selectedEyeBallShape =
                        QrVectorBallShape.AsPixelShape(QrVectorPixelShape.Circle(eyeCircleMiniSize))
                }

                eyeCircleSizeText.text = progress.toString()

                onUpdate()
            }))


            groupHighlightShape.setOnCheckedChangeListener { _, checkedId ->
                selectedTimingShape = when (checkedId) {
                    R.id.timingDefault -> QrVectorPixelShape.Default
                    R.id.timingCircle -> QrVectorPixelShape.Circle(timingCircleSize)
                    R.id.timingRoundCorners -> QrVectorPixelShape.RoundCorners(0.25f)
                    R.id.timingStar -> QrVectorPixelShape.Bubble(
                        timingBubbleSize, timingBubbleMedSize, timingBubbleMaxSize
                    )

                    R.id.timingRhombus -> QrVectorPixelShape.Rhombus(timingRhombusSize)
                    R.id.timingCircleMini -> QrVectorPixelShape.Circle(eyeCircleMiniSize)
                    else -> QrVectorPixelShape.Default
                }

                onUpdate()

                val isSliderVisible = checkedId in arrayOf(
                    R.id.timingDefault,
                    R.id.timingRoundCorners,
                    R.id.timingStar,
                    R.id.timingRhombus,
                    R.id.timingCircle
                )

                timingSliderFrame.visibility = if (isSliderVisible) VISIBLE else GONE

                if (isSliderVisible) {
                    root.postDelayed({ scrollview.fullScroll(FOCUS_DOWN) }, 200)
                }
            }



            timingSlider.setOnSeekBarChangeListener(createSeekBarListener(onProgressChanged = { progress ->
                val size = progress / 100f
                val (shape, updatedSize) = when (groupHighlightShape.checkedRadioButtonId) {
                    R.id.timingDefault -> {
                        timingSquareSize = size
                        QrVectorPixelShape.Square(size) to size
                    }

                    R.id.timingRoundCorners -> {
                        timingRoundness = size
                        QrVectorPixelShape.RoundCorners(size) to size
                    }

                    R.id.timingStar -> {
                        timingBubbleSize = size.coerceIn(0f, 0.7f)
                        timingBubbleMedSize = (size + 0.1f).coerceIn(0f, 1f)
                        timingBubbleMaxSize = (size + 0.2f).coerceIn(0f, 1f)
                        QrVectorPixelShape.Bubble(
                            timingBubbleSize, timingBubbleMedSize, timingBubbleMaxSize
                        ) to size

                    }

                    R.id.timingRhombus -> {
                        timingRhombusSize = size.coerceIn(0f, 1f)
                        QrVectorPixelShape.Rhombus(size) to size
                    }

                    else -> {
                        timingCircleSize = size
                        QrVectorPixelShape.Circle(size) to size
                    }
                }

                selectedTimingShape = shape
                timingSizeText.text = updatedSize.toString()
                onUpdate()
            }))


        }
    }


    fun logoSetting(binding: LayoutQrLogoBinding, onUpdate: () -> Unit) {
        val activity = binding.root.context as? QrGenerator


        with(binding) {

            btnImportLogo.setOnCheckedListener { isChecked ->
                if (isChecked) {
                    activity?.importDrawable(true, btnImportLogo, logoUi = binding)
                }
            }

            btnDelete.setOnCheckedListener { isChecked ->
                if (isChecked) {
                    qrLogo?.bitmap?.recycle()
                    qrLogo = null
                    btnDelete.isVisible = false
                    onUpdate()
                }
            }

            btnCenterCrop.setOnCheckedListener { isChecked ->
                centerCrop = isChecked
                onUpdate()
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

        }
    }


    fun qrColorSetting(binding: LayoutQrColorBinding, onUpdate: () -> Unit) {
        // Color pickers
        with(binding) {
            val activity = binding.root.context as? AppCompatActivity
            root.animateLayout()

            var isHightlight = false

            val colorPickerDialog = ColorPickerDialog(ui = binding).apply {
                onColorChanged = { newColor ->

                    if (useSolidColor) {
                        when (currentColorType) {
                            QrColorType.BALL -> ballColor = newColor
                            QrColorType.FRAME -> frameColor = newColor
                            QrColorType.DARK -> darkColor = newColor
                            QrColorType.LIGHT -> lightColor = newColor
                            QrColorType.QR -> {
                                darkColor = newColor
                                lightColor = newColor
                                frameColor = newColor
                                ballColor = newColor
                            }

                            QrColorType.CORNER_EYES -> {
                                cornerEyesColor = newColor
                            }

                            QrColorType.VERSION_EYES -> {
                                versionEyesColor = newColor
                            }

                            QrColorType.TIMING_LINE -> {
                                timingLinesColor = newColor
                            }

                            else -> Unit
                        }
                    } else {
                        when (currentColorType) {
                            QrColorType.COLOR0 -> if (!isHightlight) gradientColor0 =
                                newColor else gradientColor00 = newColor

                            QrColorType.COLOR1 -> if (!isHightlight) gradientColor1 =
                                newColor else gradientColor11 = newColor

                            else -> Unit
                        }
                        if (!isHightlight) {
                            QrVectorColor.LinearGradient(
                                colors = listOf(
                                    0f to gradientColor0,
                                    1f to gradientColor1,
                                ), orientation = selectedGradientOrientation
                            )
                        } else {
                            Log.i("TAG", "qrColorSetting: setting radial gradient: $newColor")
                            QrVectorColor.RadialGradient(
                                colors = listOf(
                                    0f to gradientColor00,
                                    1f to gradientColor11,
                                )

                            )

                        }
                    }
                    onUpdate()

                }
            }


            // Track the last checked group
            var lastCheckedGroup: RadioGroup? = null
            var lastCheckedId: Int = -1

            fun handleCheckedChange(group: RadioGroup, checkedId: Int, highlight: Boolean) {
                // Check if the current selection is different from the previous selection
                if (group.checkedRadioButtonId != lastCheckedId) {
                    val colorType = when (checkedId) {
                        R.id.qr_color -> QrColorType.QR
                        R.id.qr_darkpixel_color -> QrColorType.DARK
                        R.id.qr_lightpixel_color -> QrColorType.LIGHT
                        R.id.qr_eye_color -> QrColorType.BALL
                        R.id.qr_frame_color -> QrColorType.FRAME
                        R.id.corner_eyes -> QrColorType.CORNER_EYES
                        R.id.version_eyes -> QrColorType.VERSION_EYES
                        R.id.timing_line -> QrColorType.TIMING_LINE
                        else -> return
                    }

                    // Update color type and visibility
                    setColorType = colorType
                    colorTypeFrame.isVisible = true
                    isHightlight = highlight

                    // Clear the previous group only if it's different from the current one
                    if (lastCheckedGroup != group) {
                        lastCheckedGroup?.clearCheck()
                        lastCheckedGroup = group
                        btnGradientColor.isChecked = false
                        btnSolidColor.isChecked = false
                        colorChangerFrame.isVisible = false
                    }

                    // Update the last checked ID
                    lastCheckedId = checkedId
                }
            }

            // Set listeners
            radioGroupColor.setOnCheckedChangeListener { group, checkedId ->
                handleCheckedChange(group, checkedId, highlight = false)
            }

            groupHighlight.setOnCheckedChangeListener { group, checkedId ->
                handleCheckedChange(group, checkedId, highlight = true)
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
                    if (!isHightlight) {
                        gradientFrame.isVisible = true
                        btnSolidColor.isChecked = false
                        btnGradientColor1.isVisible = true
                        colorChangerFrame.isVisible = true
                        useGradientHighlight = false
                        setGradientFlags(
                            solid = false, linear = true, radial = false, sweep = false
                        )
                    } else {
                        useSolidColor = false
                        btnSolidColor.isChecked = false
                        btnGradientColor1.isVisible = true
                        colorChangerFrame.isVisible = true
                        useGradientHighlight = true
                    }
                    onUpdate()
                } else {
                    gradientFrame.isVisible = false
                    useGradientHighlight = false
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

            }




            btnAdjust.setOnCheckedListener { isChecked ->
                adjustLayout.isVisible = isChecked
            }

            btnDelete.setOnCheckedListener { isChecked ->
                if (isChecked) {
                    gifBackground = null
                    stillBackground?.bitmap?.recycle()
                    stillBackground = null
                    btnDelete.isVisible = false
                    onUpdate()
                }
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
                        effectLabel.text = "Halftone"
                    }

                    R.id.binary_effect -> {
                        useHalftone = false
                        useBinary = true
                        thresholdLayout.isVisible = true
                        activity?.updateQrBackground()
                        onUpdate()
                        effectLabel.text = "Binary"
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
                if (isChecked) {
                    if (radioGroupEffect.checkedRadioButtonId == R.id.halftone_effect){
                        effectLabel.text = "Halftone - Colored"
                    } else {
                        effectLabel.text = "Binary - Colored"
                    }
                } else {
                    if (radioGroupEffect.checkedRadioButtonId == R.id.halftone_effect){
                        effectLabel.text = "Halftone"
                    } else {
                        effectLabel.text = "Binary"
                    }
                }

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

    private fun ComponentActivity.updateQrBackground() {

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
