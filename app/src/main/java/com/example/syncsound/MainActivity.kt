package com.example.syncsound

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var projectionManager: MediaProjectionManager

    private val captureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            Toast.makeText(this, "Permission Granted!", Toast.LENGTH_SHORT).show()
            
            val serviceIntent = Intent(this, AudioCaptureService::class.java).apply {
                putExtra("RESULT_CODE", result.resultCode)
                putExtra("DATA_INTENT", result.data)
            }
            startForegroundService(serviceIntent)
        } else {
            Toast.makeText(this, "Capture permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        val startBtn: Button = findViewById(R.id.btnStart)
        startBtn.setOnClickListener {
            captureLauncher.launch(projectionManager.createScreenCaptureIntent())
        }
    }
}
