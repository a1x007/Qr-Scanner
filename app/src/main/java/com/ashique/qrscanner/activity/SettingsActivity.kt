package com.ashique.qrscanner.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ashique.qrscanner.databinding.ActivitySettingsBinding
import com.ashique.qrscanner.helper.Prefs.useZxing
import com.ashique.qrscanner.helper.Prefs.initialize
import com.ashique.qrscanner.helper.Prefs.setZxing

class SettingsActivity : AppCompatActivity() {

    private lateinit var ui: ActivitySettingsBinding




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(ui.root)

        initialize(this) // Prefs

        ui.switchZxing.isChecked = useZxing()

        ui.switchZxing.setOnCheckedChangeListener { _, isChecked ->
           setZxing(isChecked)
        }
    }
}
