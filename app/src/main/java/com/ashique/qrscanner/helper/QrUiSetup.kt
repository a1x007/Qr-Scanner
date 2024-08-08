package com.ashique.qrscanner.helper

import android.graphics.Color
import android.view.View
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import codes.side.andcolorpicker.converter.toColorInt
import com.ashique.qrscanner.R
import com.ashique.qrscanner.activity.QrGenerator.Companion.ballColor
import com.ashique.qrscanner.activity.QrGenerator.Companion.ballRoundness
import com.ashique.qrscanner.activity.QrGenerator.Companion.currentColorType
import com.ashique.qrscanner.activity.QrGenerator.Companion.darkColor
import com.ashique.qrscanner.activity.QrGenerator.Companion.darkPixelRoundness
import com.ashique.qrscanner.activity.QrGenerator.Companion.frameColor
import com.ashique.qrscanner.activity.QrGenerator.Companion.frameRoundness
import com.ashique.qrscanner.activity.QrGenerator.Companion.gradientColor0
import com.ashique.qrscanner.activity.QrGenerator.Companion.gradientColor1
import com.ashique.qrscanner.activity.QrGenerator.Companion.logoPadding
import com.ashique.qrscanner.activity.QrGenerator.Companion.logoSize
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
import com.ashique.qrscanner.databinding.LayoutQrColorBinding
import com.ashique.qrscanner.databinding.LayoutQrLogoBinding
import com.ashique.qrscanner.databinding.LayoutQrShapeBinding
import com.ashique.qrscanner.helper.Extensions.animateViewFromBottom
import com.github.alexzhirkevich.customqrgenerator.style.QrShape
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorBallShape
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorColor
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorFrameShape
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorPixelShape

object QrUiSetup {

    private var circleSize: Float = 0.20f
    fun shapeSetting(binding: LayoutQrShapeBinding, onUpdate: () -> Unit) {

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
                R.id.pixelCircle -> QrVectorPixelShape.Circle()
                R.id.pixelRoundCorners25 -> QrVectorPixelShape.RoundCorners(0.25f)
                R.id.pixelStar -> QrVectorPixelShape.Star
                R.id.pixelRhombus -> QrVectorPixelShape.Rhombus()
                R.id.pixelRoundCornersHorizontal -> QrVectorPixelShape.RoundCornersHorizontal()
                R.id.radioButtonRoundCornersVertical -> QrVectorPixelShape.RoundCornersVertical()
                else -> QrVectorPixelShape.Default // Default fallback
            }
            onUpdate()
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
                        .20f
                    )
                )

                else -> QrVectorFrameShape.Default// Default fallback
            }
            onUpdate()
        }

        binding.radioGroupEyeShape.setOnCheckedChangeListener { _, checkedId ->
            selectedEyeBallShape = when (checkedId) {
                R.id.eyeDefault -> QrVectorBallShape.Default
                R.id.eyeCircle -> QrVectorBallShape.Circle(0.30f)
                R.id.eyeRoundCorners25 -> QrVectorBallShape.RoundCorners(0.25f)
                R.id.eyeStar -> QrVectorBallShape.AsPixelShape(QrVectorPixelShape.Star)
                R.id.eyeRhombus -> QrVectorBallShape.AsPixelShape(QrVectorPixelShape.Rhombus())
                R.id.eyeCircleMini -> QrVectorBallShape.AsPixelShape(QrVectorPixelShape.Circle(.20f))
                else -> QrVectorBallShape.Default// Default fallback
            }

            onUpdate()
            // Hide the slider when other shapes are selected
            binding.circleSizeSlider.visibility = if (checkedId == R.id.eyeCircleMini)
                View.VISIBLE
            else View.GONE
        }

        binding.circleSizeSlider.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                circleSize = progress / 100f // Convert progress to a float between 0.0 and 1.0
                binding.circleSizeText.text = "Circle Size: %.2f".format(circleSize)

                // Update the selected shape live to reflect the new size
                if (binding.radioGroupEyeShape.checkedRadioButtonId == R.id.eyeCircleMini) {
                    selectedEyeBallShape =
                        QrVectorBallShape.AsPixelShape(QrVectorPixelShape.Circle(circleSize))
                    onUpdate()
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }


    fun logoSetting(binding: LayoutQrLogoBinding, onUpdate: () -> Unit) {
        // Sliders
        binding.paddingSlider.setOnSeekBarChangeListener(createSeekBarListener { progress ->
            // Update padding in the context of the activity
            logoPadding = progress / 100f
            onUpdate()
        })

        binding.logoSizeSlider.setOnSeekBarChangeListener(createSeekBarListener { progress ->
            logoSize = progress / 100f
            onUpdate()
        })

        binding.darkPixelRoundnessSlider.setOnSeekBarChangeListener(createSeekBarListener { progress ->
            darkPixelRoundness = progress / 100f
            selectedDarkPixelShape = QrVectorPixelShape.RoundCorners(darkPixelRoundness)

            onUpdate()
        })

        binding.ballRoundnessSlider.setOnSeekBarChangeListener(createSeekBarListener { progress ->
            ballRoundness = progress / 100f
            QrVectorBallShape.RoundCorners(ballRoundness)
            onUpdate()
        })

        binding.frameRoundnessSlider.setOnSeekBarChangeListener(createSeekBarListener { progress ->
            frameRoundness = progress / 100f
            QrVectorFrameShape.RoundCorners(frameRoundness)
            onUpdate()
        })

    }


    /*fun qrColorSetting(binding: LayoutQrColorBinding, onUpdate: () -> Unit) {
        // Color pickers
        binding.btnBallColor.setOnClickListener {
            currentColorType = ColorPicker.QrColorType.BALL
            binding.colorPicker.showHide(true)
        }

        binding.btnFrameColor.setOnClickListener {
            currentColorType = ColorPicker.QrColorType.FRAME
            binding.colorPicker.showHide(true)
        }

        binding.btnQrColor.setOnClickListener {
            currentColorType = ColorPicker.QrColorType.DARK
            binding.colorPicker.showHide(true)
        }

        binding.btnConfirmColor.setOnClickListener {
            binding.colorPicker.showHide(false)
        }

        // Setup the color pickers
        binding.hueSeekBar.setupColorPickers(
            binding.hueSeekBar,
            binding.saturationSeekBar,
            binding.lightnessSeekBar,
            binding.alphaSeekBar
        ) { newColor ->
            when (currentColorType) {
                ColorPicker.QrColorType.BALL -> ballColor = newColor.toColorInt()
                ColorPicker.QrColorType.FRAME -> frameColor = newColor.toColorInt()
                ColorPicker.QrColorType.DARK -> darkColor = newColor.toColorInt()
                else -> Unit
            }

            onUpdate()

            binding.swatchView.setSwatchColor(newColor) // Update swatch view color
        }

        binding.btnConfirmColor.setOnClickListener { binding.colorPicker.showHide(false) }
    }


     */

    fun qrColorSetting(binding: LayoutQrColorBinding, onUpdate: () -> Unit) {
        // Color pickers
        with(binding) {
            val activity = binding.root.context as? AppCompatActivity

            val colorPickerDialog = ColorPickerDialog().apply {
                onColorChanged = { newColor ->
                    //binding.root.context.showToast("newColor: $newColor")
                    if (useSolidColor) {
                        when (currentColorType) {
                            ColorPicker.QrColorType.BALL -> ballColor = newColor
                            ColorPicker.QrColorType.FRAME -> frameColor = newColor
                            ColorPicker.QrColorType.DARK -> darkColor = newColor
                            ColorPicker.QrColorType.QR -> {
                                darkColor = newColor
                                frameColor = newColor
                                ballColor = newColor
                            }
                            else -> Unit
                        }
                    } else if (useLinearGradient) {
                        when (currentColorType) {
                            ColorPicker.QrColorType.COLOR0 -> gradientColor0 = newColor
                            ColorPicker.QrColorType.COLOR1 -> gradientColor1 = newColor
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
                        currentColorType = ColorPicker.QrColorType.QR
                        activity?.supportFragmentManager?.let {
                            colorPickerDialog.show(it, "ColorPickerDialog")
                        }
                        //colorPickerView.animateViewFromBottom(true)
                    }

                    R.id.qr_darkpixel_color -> {
                        currentColorType = ColorPicker.QrColorType.DARK
                        activity?.supportFragmentManager?.let {
                            colorPickerDialog.show(it, "ColorPickerDialog")
                        }
                      //  colorPickerView.animateViewFromBottom(true)
                    }

                    R.id.qr_eye_color -> {
                        currentColorType = ColorPicker.QrColorType.BALL
                        activity?.supportFragmentManager?.let {
                            colorPickerDialog.show(it, "ColorPickerDialog")
                        }
                       // colorPickerView.animateViewFromBottom(true)
                    }

                    R.id.qr_frame_color -> {
                        currentColorType = ColorPicker.QrColorType.FRAME
                        activity?.supportFragmentManager?.let {
                            colorPickerDialog.show(it, "ColorPickerDialog")
                        }
                        //colorPickerView.animateViewFromBottom(true)
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
                        setGradientFlags(false, true, false, false)
                    }

                    R.id.radial -> {
                        setGradientFlags(false, false, true, false)
                    }

                    R.id.sweep -> {
                        setGradientFlags(false, false, false, true)
                    }

                    else -> {
                        setGradientFlags(false, true, false, false)
                    }
                }
                onUpdate()
            }







            colorPickerView.onColorChanged = { newColor ->

                if (useSolidColor) {
                    when (currentColorType) {
                        ColorPicker.QrColorType.BALL -> ballColor = newColor.toColorInt()
                        ColorPicker.QrColorType.FRAME -> frameColor = newColor.toColorInt()
                        ColorPicker.QrColorType.DARK -> darkColor = newColor.toColorInt()
                        ColorPicker.QrColorType.QR -> {
                            darkColor = newColor.toColorInt()
                            frameColor = newColor.toColorInt()
                            ballColor = newColor.toColorInt()
                        }
                        else -> Unit
                    }
                } else if (useLinearGradient) {
                    when (currentColorType) {
                        ColorPicker.QrColorType.COLOR0 -> gradientColor0 = newColor.toColorInt()
                        ColorPicker.QrColorType.COLOR1 -> gradientColor1 = newColor.toColorInt()
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

                colorPickerView.swatchView?.setSwatchColor(newColor)


            }

            btnSolidColor.setOnClickListener {
                setGradientFlags(true, false, false, false)
                onUpdate()
            }

            btnGradientColor.setOnClickListener {
                setGradientFlags(false, true, false, false)
                onUpdate()
            }

            btnGradientColor0.setOnCheckedListener { isChecked ->
                if (isChecked) {
                    btnGradientColor0.apply {
                        setBgColor(Color.WHITE)
                        setIconColor(Color.BLACK)
                    }
                    currentColorType = ColorPicker.QrColorType.COLOR0
                    colorPickerView.animateViewFromBottom(true)
                }

            }


            btnGradientColor1.setOnCheckedListener { isChecked ->
                if (isChecked) {
                    btnGradientColor1.apply {
                        setBgColor(Color.WHITE)
                        setIconColor(Color.BLACK)
                    }
                    currentColorType = ColorPicker.QrColorType.COLOR1
                    colorPickerView.animateViewFromBottom(true)
                }

            }
        }
    }


    private fun createSeekBarListener(onProgressChanged: (Int) -> Unit): SeekBar.OnSeekBarChangeListener {
        return object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                onProgressChanged(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        }
    }

    private fun setGradientFlags(solid: Boolean, linear: Boolean, radial: Boolean, sweep: Boolean) {
        useSolidColor = solid
        useLinearGradient = linear
        useRadialGradient = radial
        useSweepGradient = sweep
    }

}
