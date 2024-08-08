package com.ashique.qrscanner.helper

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.widget.LinearLayout
import codes.side.andcolorpicker.converter.IntegerHSLColorConverter
import codes.side.andcolorpicker.group.PickerGroup
import codes.side.andcolorpicker.group.registerPickers
import codes.side.andcolorpicker.model.IntegerHSLColor
import codes.side.andcolorpicker.view.picker.ColorSeekBar
import codes.side.andcolorpicker.view.picker.OnIntegerHSLColorPickListener
import codes.side.andcolorpicker.view.swatch.SwatchView
import com.ashique.qrscanner.R
import com.ashique.qrscanner.custom.Frame
import com.ashique.qrscanner.custom.HSLColorPickerSeekBar
import com.ashique.qrscanner.helper.Extensions.animateViewFromBottom

class ColorPickerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val hueSeekBar: HSLColorPickerSeekBar
    private val saturationSeekBar: ColorSeekBar<IntegerHSLColor>
    private val lightnessSeekBar: ColorSeekBar<IntegerHSLColor>
    private val alphaSeekBar: ColorSeekBar<IntegerHSLColor>
    var swatchView: SwatchView? = null

    private var selectedColor: Int? = null
    var onColorChanged: ((IntegerHSLColor) -> Unit)? = null

    init {
        // Inflate the layout  
        LayoutInflater.from(context).inflate(R.layout.color_picker_view, this, true)

        // Initialize the seek bars  
        hueSeekBar = findViewById(R.id.hueSeekBar)
        saturationSeekBar = findViewById(R.id.saturationSeekBar)
        lightnessSeekBar = findViewById(R.id.lightnessSeekBar)
        alphaSeekBar = findViewById(R.id.alphaSeekBar)
        swatchView = findViewById(R.id.swatchView)
        findViewById<Frame>(R.id.btn_confirm_color).setOnClickListener {
            this.animateViewFromBottom(
                false
            )
        }
        setupColorPickers()
    }

    private fun setupColorPickers() {
        // Configure picker color model programmatically  
        hueSeekBar.mode = HSLColorPickerSeekBar.Mode.MODE_HUE
        hueSeekBar.coloringMode = HSLColorPickerSeekBar.ColoringMode.PURE_COLOR

        // Group pickers with PickerGroup to automatically synchronize color across them  
        val pickerGroup = PickerGroup<IntegerHSLColor>().also {
            it.registerPickers(hueSeekBar, saturationSeekBar, lightnessSeekBar, alphaSeekBar)
        }

        // Listen to color changes  
        pickerGroup.addListener(object : OnIntegerHSLColorPickListener() {
            override fun onColorChanged(
                picker: ColorSeekBar<IntegerHSLColor>,
                color: IntegerHSLColor,
                value: Int
            ) {
                Log.d("ColorPickerView", "$color picked")
                val convertedColor = IntegerHSLColorConverter().convertToColorInt(color)
                onColorChanged?.invoke(color)
                //selectedColor = convertedColor
            }
        })
    }

    fun getSelectedColor(): ((IntegerHSLColor) -> Unit)? {
        // Return the currently selected color  
        return onColorChanged
    }
}