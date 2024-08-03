package com.ashique.qrscanner.helper

import android.util.Log
import android.view.View
import codes.side.andcolorpicker.converter.IntegerHSLColorConverter
import codes.side.andcolorpicker.group.PickerGroup
import codes.side.andcolorpicker.group.registerPickers
//import codes.side.andcolorpicker.hsl.HSLColorPickerSeekBar
import com.ashique.qrscanner.custom.HSLColorPickerSeekBar
import codes.side.andcolorpicker.model.IntegerHSLColor
import codes.side.andcolorpicker.view.picker.ColorSeekBar
import codes.side.andcolorpicker.view.picker.OnIntegerHSLColorPickListener

object ColorPicker {
    enum class QrColorType {
        BALL, FRAME, DARK
    }


    fun View.setupColorPickers(
        hueSeekBar: HSLColorPickerSeekBar,
        saturationSeekBar: ColorSeekBar<IntegerHSLColor>,
        lightnessSeekBar: ColorSeekBar<IntegerHSLColor>,
        alphaSeekBar: ColorSeekBar<IntegerHSLColor>,
        onColorChanged: (color: IntegerHSLColor) -> Unit
    ) {
        // Configure picker color model programmatically
        hueSeekBar.mode = HSLColorPickerSeekBar.Mode.MODE_HUE

        // Configure coloring mode programmatically
        hueSeekBar.coloringMode = HSLColorPickerSeekBar.ColoringMode.PURE_COLOR

        // Group pickers with PickerGroup to automatically synchronize color across them
        val pickerGroup = PickerGroup<IntegerHSLColor>().also {
            it.registerPickers(hueSeekBar, saturationSeekBar, lightnessSeekBar, alphaSeekBar)
        }

        // Listen individual pickers or groups for changes
        pickerGroup.addListener(object : OnIntegerHSLColorPickListener() {
            override fun onColorChanged(
                picker: ColorSeekBar<IntegerHSLColor>,
                color: IntegerHSLColor,
                value: Int
            ) {
                Log.d("ColorPicker", "$color picked")

                val convertedColor = IntegerHSLColorConverter().convertToColorInt(color)

                // Call the callback with the new color
                onColorChanged(color)

                Log.d("ColorPicker", "Current color is $convertedColor")
            }
        })
    }

}