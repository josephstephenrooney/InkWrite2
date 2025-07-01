package com.example.screenshotapp

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private var isCapturing = false
    private lateinit var startButton: Button
    private lateinit var stopButton: Button

    private val mediaProjectionManager by lazy {
        getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        startButton = findViewById(R.id.btnStart)
        stopButton = findViewById(R.id.btnStop)

        isCapturing = savedInstanceState?.getBoolean(KEY_IS_CAPTURING) ?: false
        updateButtons()

        startButton.setOnClickListener {
            if (!isCapturing) {
                val intent = mediaProjectionManager.createScreenCaptureIntent()
                startActivityForResult(intent, REQUEST_SCREENSHOT)
            }
        }

        stopButton.setOnClickListener {
            stopCapture()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_SCREENSHOT && resultCode == Activity.RESULT_OK) {
            val serviceIntent = Intent(this, ScreenshotService::class.java).apply {
                putExtra(ScreenshotService.EXTRA_RESULT_CODE, resultCode)
                putExtra(ScreenshotService.EXTRA_RESULT_DATA, data)
                action = ScreenshotService.ACTION_START
            }
            startForegroundService(serviceIntent)
            isCapturing = true
            updateButtons()
        }
    }

    private fun stopCapture() {
        val intent = Intent(this, ScreenshotService::class.java).apply {
            action = ScreenshotService.ACTION_STOP
        }
        stopService(intent)
        isCapturing = false
        updateButtons()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_IS_CAPTURING, isCapturing)
    }

    private fun updateButtons() {
        startButton.isEnabled = !isCapturing
        stopButton.isEnabled = isCapturing
    }

    companion object {
        private const val REQUEST_SCREENSHOT = 100
        private const val KEY_IS_CAPTURING = "key_is_capturing"
    }
}
