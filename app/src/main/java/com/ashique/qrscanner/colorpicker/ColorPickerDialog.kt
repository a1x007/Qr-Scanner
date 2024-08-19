package com.ashique.qrscanner.colorpicker

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.ashique.qrscanner.databinding.DialogColorPickerBinding
import com.ashique.qrscanner.databinding.LayoutQrBackgroundBinding
import com.ashique.qrscanner.databinding.LayoutQrColorBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ColorPickerDialog(
    private val ui: LayoutQrColorBinding? = null,
    private val ui2: LayoutQrBackgroundBinding? = null
) : DialogFragment() {

    private var _binding: DialogColorPickerBinding? = null
    private val binding get() = _binding!!

    var onColorChanged: ((Int) -> Unit)? = null

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setCanceledOnTouchOutside(false)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        return dialog.apply {
            setTitle("Pick a Color")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogColorPickerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set up color picker view
        binding.colorPickerView.alphaSliderView = binding.colorAlphaSlider
        binding.colorPickerView.hueSliderView = binding.hueSlider

        // Handle color changes
        binding.colorPickerView.setOnColorChangedListener { color ->
            coroutineScope.launch {
                // Perform background tasks if needed
                // For example: processColorChange(color)
                onColorChanged?.invoke(color)
                // Update the preview button color on the main thread
                updatePreviewButtonColor(color)
            }
        }

        // Handle preview button click
        binding.previewButton.setOnClickListener {
            ui?.btnGradientColor0?.isChecked = false
            ui?.btnGradientColor1?.isChecked = false
            dismiss()
        }
    }

    private suspend fun updatePreviewButtonColor(color: Int) {
        withContext(Dispatchers.Main) {
            binding.previewButton.setIconColor(color)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
