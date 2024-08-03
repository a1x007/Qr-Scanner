package com.ashique.qrscanner.helper
import android.widget.SeekBar
import codes.side.andcolorpicker.converter.toColorInt
import com.ashique.qrscanner.R
import com.ashique.qrscanner.activity.QrGenerator.Companion.ballColor
import com.ashique.qrscanner.activity.QrGenerator.Companion.ballRoundness
import com.ashique.qrscanner.activity.QrGenerator.Companion.currentColorType
import com.ashique.qrscanner.activity.QrGenerator.Companion.darkColor
import com.ashique.qrscanner.activity.QrGenerator.Companion.darkPixelRoundness
import com.ashique.qrscanner.activity.QrGenerator.Companion.frameColor
import com.ashique.qrscanner.activity.QrGenerator.Companion.frameRoundness
import com.ashique.qrscanner.activity.QrGenerator.Companion.logoPadding
import com.ashique.qrscanner.activity.QrGenerator.Companion.logoSize
import com.ashique.qrscanner.activity.QrGenerator.Companion.selectedDarkPixelShape
import com.ashique.qrscanner.databinding.LayoutQrColorBinding
import com.ashique.qrscanner.databinding.LayoutQrLogoBinding
import com.ashique.qrscanner.databinding.LayoutQrShapeBinding
import com.ashique.qrscanner.databinding.QrGeneratorBinding
import com.ashique.qrscanner.helper.ColorPicker.setupColorPickers
import com.ashique.qrscanner.helper.Extensions.showHide
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorPixelShape

object QrUiSetup {

    fun pixelShapeSetting(binding: LayoutQrShapeBinding, onUpdate: () -> Unit){
        binding.radioGroupDarkPixelShape.setOnCheckedChangeListener { _, checkedId ->
            selectedDarkPixelShape = when (checkedId) {
                R.id.radioButtonDefault -> QrVectorPixelShape.Default
                R.id.radioButtonCircle -> QrVectorPixelShape.Circle()
                R.id.radioButtonRoundCorners25 -> QrVectorPixelShape.RoundCorners(0.25f)
                R.id.radioButtonStar -> QrVectorPixelShape.Star
                R.id.radioButtonRhombus -> QrVectorPixelShape.Rhombus()
                R.id.radioButtonRoundCornersHorizontal -> QrVectorPixelShape.RoundCornersHorizontal()
                R.id.radioButtonRoundCornersVertical -> QrVectorPixelShape.RoundCornersVertical()
                else -> QrVectorPixelShape.Default // Default fallback
            }
            onUpdate()
        }
    }

    fun logoSetting(binding: LayoutQrLogoBinding, onUpdate: () -> Unit){
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
            onUpdate()
        })

        binding.ballRoundnessSlider.setOnSeekBarChangeListener(createSeekBarListener { progress ->
            ballRoundness = progress / 100f
            onUpdate()
        })

        binding.frameRoundnessSlider.setOnSeekBarChangeListener(createSeekBarListener { progress ->
            frameRoundness = progress / 100f
            onUpdate()
        })

    }


    fun qrColorSetting(binding: LayoutQrColorBinding, onUpdate: () -> Unit){
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

    fun setupUi(binding: QrGeneratorBinding, onUpdate: () -> Unit) {


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


}
