package com.entangle.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ShareActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Handle the incoming share intent immediately
        when {
            intent.action == Intent.ACTION_SEND -> {
                handleSingleShare(intent)
            }
            intent.action == Intent.ACTION_SEND_MULTIPLE -> {
                handleMultipleShare(intent)
            }
            else -> {
                showToast("Unsupported share type")
                finish()
            }
        }
    }

    private fun handleSingleShare(intent: Intent) {
        val mimeType = intent.type ?: "*/*"

        when {
            mimeType == "text/plain" -> {
                // Text or URL
                val text = intent.getStringExtra(Intent.EXTRA_TEXT)
                val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT)

                if (text != null) {
                    startTransfer(TransferData.Text(text, subject))
                } else {
                    showToast("No text to share")
                    finish()
                }
            }
            else -> {
                // File — image, video, document, etc.
                val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                if (uri != null) {
                    startTransfer(TransferData.File(uri, mimeType))
                } else {
                    showToast("No file to share")
                    finish()
                }
            }
        }
    }

    private fun handleMultipleShare(intent: Intent) {
        val uris = intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
        if (!uris.isNullOrEmpty()) {
            // Transfer files one by one
            uris.forEach { uri ->
                startTransfer(TransferData.File(uri, intent.type ?: "*/*"))
            }
        } else {
            finish()
        }
    }

    private fun startTransfer(data: TransferData) {
        // Start background service for transfer
        val serviceIntent = Intent(this, TransferService::class.java).apply {
            putExtra("transfer_data", data.toBundle())
        }
        startService(serviceIntent)

        // Show brief toast and close immediately
        // The transfer happens in the background
        showToast("Sending to laptop...")
        finish() // CRITICAL: dismiss immediately — don't block the user
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}

// Data classes for transfer types
sealed class TransferData {
    data class Text(val content: String, val subject: String?) : TransferData()
    data class File(val uri: Uri, val mimeType: String) : TransferData()

    fun toBundle(): android.os.Bundle {
        return android.os.Bundle().apply {
            when (this@TransferData) {
                is Text -> {
                    putString("type", "text")
                    putString("content", content)
                    putString("subject", subject)
                }
                is File -> {
                    putString("type", "file")
                    putParcelable("uri", uri)
                    putString("mimeType", mimeType)
                }
            }
        }
    }
}
