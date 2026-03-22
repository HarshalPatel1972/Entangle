package com.entangle.app

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Dynamic layout setup for simplicity without relying on XML layout files
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(64, 64, 64, 64)
            gravity = android.view.Gravity.CENTER
        }
        
        val titleText = TextView(this).apply {
            text = "Entangle"
            textSize = 32f
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = android.view.Gravity.CENTER
        }
        
        val instructionText = TextView(this).apply {
            text = "\nShare anything from any app — Entangle will appear in the share menu.\n"
            textSize = 16f
            gravity = android.view.Gravity.CENTER
        }
        
        val statusText = TextView(this).apply {
            text = "Status: Waiting to test..."
            textSize = 14f
            gravity = android.view.Gravity.CENTER
        }

        val testButton = Button(this).apply {
            text = "Test Connection"
            setOnClickListener {
                statusText.text = "Searching for laptop..."
                lifecycleScope.launch {
                    val desktop = kotlinx.coroutines.withTimeoutOrNull(8000) { 
                        findDesktop(this@MainActivity) 
                    }
                    if (desktop != null) {
                        val verified = verifyDesktop(desktop.host, desktop.port)
                        if (verified) {
                            statusText.text = "Connected to ${desktop.host}:${desktop.port}"
                        } else {
                            statusText.text = "Found ${desktop.host}, but couldn't verify."
                        }
                    } else {
                        statusText.text = "Could not find laptop on network."
                    }
                }
            }
        }
        
        val versionText = TextView(this).apply {
            text = "\nVersion 1.0"
            textSize = 12f
            gravity = android.view.Gravity.CENTER
            setTextColor(android.graphics.Color.GRAY)
        }

        layout.addView(titleText)
        layout.addView(instructionText)
        layout.addView(testButton)
        layout.addView(statusText)
        layout.addView(versionText)

        setContentView(layout)
    }
}
