package com.ashique.qrscanner.activity

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ashique.qrscanner.databinding.ActivityResultBinding

class ResultActivity : AppCompatActivity() {

    private lateinit var ui: ActivityResultBinding

    companion object {
        const val EXTRA_RESULT_URL = "extra_result_url"

        fun newIntent(context: Context, resultUrl: String): Intent {
            return Intent(context, ResultActivity::class.java).apply {
                putExtra(EXTRA_RESULT_URL, resultUrl)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = ActivityResultBinding.inflate(layoutInflater)
        setContentView(ui.root)

        val resultUrl = intent.getStringExtra(EXTRA_RESULT_URL)

        ui.resultTextView.text = resultUrl

        ui.resultTextView.setOnLongClickListener {
            copyToClipboard(resultUrl)
            true
        }
    }

    private fun copyToClipboard(text: String?) {
        text?.let {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("QR Code Result", it)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show()
        }
    }
}
