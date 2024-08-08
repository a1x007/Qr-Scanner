package com.ashique.qrscanner.colorpicker

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.ashique.qrscanner.databinding.DialogColorPickerBinding

class ColorPickerDialog : DialogFragment() {

    private var _binding: DialogColorPickerBinding? = null
    private val binding get() = _binding!!

    var onColorChanged: ((Int) -> Unit)? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setCanceledOnTouchOutside(false) // Prevent closing on outside touch
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        return dialog.apply {
            setTitle("Pick a Color")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = DialogColorPickerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.colorPickerView.alphaSliderView = binding.colorAlphaSlider
        binding.colorPickerView.hueSliderViews = binding.hueSlider

        binding.colorPickerView.setOnColorChangedListener { color ->
            onColorChanged?.invoke(color)

            binding.previewButton.setBgColor(color)


        }
        binding.previewButton.setOnClickListener {
            dismiss()
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
