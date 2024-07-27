package com.ashique.qrscanner.activity


import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.widget.addTextChangedListener
import codes.side.andcolorpicker.converter.IntegerHSLColorConverter
import codes.side.andcolorpicker.converter.IntegerRGBColorConverter
import codes.side.andcolorpicker.converter.toColorInt
import codes.side.andcolorpicker.group.PickerGroup
import codes.side.andcolorpicker.group.registerPickers
import codes.side.andcolorpicker.hsl.HSLColorPickerSeekBar
import codes.side.andcolorpicker.model.IntegerHSLColor
import codes.side.andcolorpicker.view.picker.ColorSeekBar
import codes.side.andcolorpicker.view.picker.OnIntegerHSLColorPickListener
import com.ashique.qrscanner.R
import com.ashique.qrscanner.databinding.QrGeneratorBinding
import com.github.alexzhirkevich.customqrgenerator.style.Color
import com.github.alexzhirkevich.customqrgenerator.vector.QrCodeDrawable
import com.github.alexzhirkevich.customqrgenerator.vector.createQrVectorOptions
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorBallShape
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorColor
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorFrameShape
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorLogoPadding
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorLogoShape
import com.github.alexzhirkevich.customqrgenerator.vector.style.QrVectorPixelShape
import me.jfenn.colorpickerdialog.dialogs.ColorPickerDialog
import me.jfenn.colorpickerdialog.interfaces.OnColorPickedListener
import me.jfenn.colorpickerdialog.views.picker.HSVPickerView

class QrGenerator : AppCompatActivity(), OnColorPickedListener<ColorPickerDialog> {

    private val binding by lazy {
        QrGeneratorBinding.inflate(layoutInflater)
    }

    private var defaultColor = Color.BLUE

    private val options by lazy {
        createQrVectorOptions {

            padding = .325f

            fourthEyeEnabled = true

            background {
                drawable = ContextCompat.getDrawable(this@QrGenerator, R.drawable.frame)
            }

            logo {
                drawable = ContextCompat.getDrawable(this@QrGenerator, R.drawable.tg)
                size = .25f
                padding = QrVectorLogoPadding.Natural(.2f)
                shape = QrVectorLogoShape.Circle
            }
            colors {
                // dark = QrVectorColor.Solid(Color(0xff345288))
                dark = QrVectorColor.Solid(defaultColor)
            }
            shapes {
                darkPixel = QrVectorPixelShape.RoundCorners(.5f)
                ball = QrVectorBallShape.RoundCorners(.25f)
                frame = QrVectorFrameShape.RoundCorners(.25f)

            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        with(binding) {
            editInput.setText(R.string.app_name)

            ivQrcode.setImageBitmap(
                QrCodeDrawable({ editInput.text.toString() }, options).toBitmap(1024, 1024),
            )

            editInput.addTextChangedListener {
                val text = editInput.text.toString()

                ivQrcode.setImageDrawable(
                    QrCodeDrawable({ text }, options),
                )
            }
            btnCreate.setOnClickListener {
                ivQrcode.setImageDrawable(
                    QrCodeDrawable(
                        {
                            editInput.text.toString()
                        }, options
                    ),
                )
            }

            btnColorpicker.setOnClickListener {
                ColorPickerDialog().withColor(defaultColor)
                    .withRetainInstance(false)
                    .withTitle("Choose a color")
                    .withCornerRadius(16f)
                    .withAlphaEnabled(true)
                    .clearPickers()
                    .withPicker(HSVPickerView::class.java)
                    .withTheme(R.style.ColorPickerDialog_Dark)
                    .withListener(this@QrGenerator)
                    .show(supportFragmentManager, "colorPicker")


            }


            // Configure picker color model programmatically
            hueSeekBar.mode =
                HSLColorPickerSeekBar.Mode.MODE_HUE // Mode.MODE_SATURATION, Mode.MODE_LIGHTNESS

            // Configure coloring mode programmatically
            hueSeekBar.coloringMode =
                HSLColorPickerSeekBar.ColoringMode.PURE_COLOR // ColoringMode.OUTPUT_COLOR

            // Group pickers with PickerGroup to automatically synchronize color across them
            val pickerGroup = PickerGroup<IntegerHSLColor>().also {
                it.registerPickers(
                    hueSeekBar,
                    saturationSeekBar,
                    lightnessSeekBar,
                    alphaSeekBar
                )
            }
            // Listen individual pickers or groups for changes
            pickerGroup.addListener(
                object : OnIntegerHSLColorPickListener() {
                    override fun onColorChanged(
                        picker: ColorSeekBar<IntegerHSLColor>,
                        color: IntegerHSLColor,
                        value: Int
                    ) {
                        Log.d(
                            "QrGenerator",
                            "$color picked"
                        )

                        defaultColor = color.toColorInt()



                        // Update the QR code options with the new color
                        val updatedOptions = createQrVectorOptions {
                            padding = .325f
                            fourthEyeEnabled = true

                            background {
                                drawable = ContextCompat.getDrawable(this@QrGenerator, R.drawable.frame)
                            }

                            logo {
                                drawable = ContextCompat.getDrawable(this@QrGenerator, R.drawable.tg)
                                size = .25f
                                padding = QrVectorLogoPadding.Natural(.2f)
                                shape = QrVectorLogoShape.Circle
                            }

                            colors {
                              //  dark = QrVectorColor.Solid(defaultColor)
                               // ball = QrVectorColor.Solid(defaultColor)
                               frame = QrVectorColor.Solid(defaultColor)
                            }

                            shapes {
                                darkPixel = QrVectorPixelShape.RoundCorners(.5f)
                                ball = QrVectorBallShape.RoundCorners(.25f)
                                frame = QrVectorFrameShape.RoundCorners(.25f)
                            }
                        }

                        // Rebuild the QR code with updated options
                        ivQrcode.setImageBitmap(
                            QrCodeDrawable({ editInput.text.toString() }, updatedOptions).toBitmap(1024, 1024)
                        )

                      val convertedColor =  IntegerHSLColorConverter().convertToColorInt(color)// Convert HSL to RGB
                        // Get current color immediately
                        Log.d(
                            "QrGenerator",
                            "Current color is ${hueSeekBar.pickedColor} / ${convertedColor}"
                        )

                        swatchView.setSwatchColor(
                            color
                        )


                    }
                }
            )

        }


    }

    override fun onColorPicked(pickerView: ColorPickerDialog?, color: Int) {
        Toast.makeText(this, String.format("#%08X", color), Toast.LENGTH_SHORT).show();
        this.defaultColor = color
    }
}