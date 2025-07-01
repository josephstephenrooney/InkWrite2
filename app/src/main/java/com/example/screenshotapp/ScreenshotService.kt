package com.example.screenshotapp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ScreenshotService : Service() {

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null

    private var handlerThread: HandlerThread? = null
    private var handler: Handler? = null
    private var takingScreenshots = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
                val data = intent.getParcelableExtra<Intent>(EXTRA_RESULT_DATA)
                startCapture(resultCode, data)
            }
            ACTION_STOP -> stopCapture()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        stopCapture()
        super.onDestroy()
    }

    private fun startCapture(resultCode: Int, data: Intent?) {
        if (takingScreenshots) return
        val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = manager.getMediaProjection(resultCode, data!!)
        setupVirtualDisplay()
        startForeground(NOTIFICATION_ID, createNotification())
        takingScreenshots = true
        handlerThread = HandlerThread("ScreenshotThread").apply { start() }
        handler = Handler(handlerThread!!.looper)
        handler?.post(screenshotRunnable)
    }

    private fun stopCapture() {
        if (!takingScreenshots) return
        takingScreenshots = false
        handler?.removeCallbacksAndMessages(null)
        handlerThread?.quitSafely()
        virtualDisplay?.release()
        imageReader?.close()
        mediaProjection?.stop()
        stopForeground(true)
        stopSelf()
    }

    private val screenshotRunnable = object : Runnable {
        override fun run() {
            if (!takingScreenshots) return
            captureScreenshot()
            handler?.postDelayed(this, INTERVAL_MS)
        }
    }

    private fun setupVirtualDisplay() {
        val metrics = DisplayMetrics()
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            display?.apply { getRealMetrics(metrics) }
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(metrics)
        }
        imageReader = ImageReader.newInstance(metrics.widthPixels, metrics.heightPixels, PixelFormat.RGBA_8888, 2)
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "ScreenCapture",
            metrics.widthPixels,
            metrics.heightPixels,
            metrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface,
            null,
            handler
        )
    }

    private fun captureScreenshot() {
        val reader = imageReader ?: return
        val image = reader.acquireLatestImage() ?: return
        val planes = image.planes
        val buffer: ByteBuffer = planes[0].buffer
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * image.width
        val bitmap = android.graphics.Bitmap.createBitmap(
            image.width + rowPadding / pixelStride,
            image.height,
            android.graphics.Bitmap.Config.ARGB_8888
        )
        bitmap.copyPixelsFromBuffer(buffer)
        image.close()
        saveBitmap(bitmap)
        bitmap.recycle()
    }

    private fun saveBitmap(bitmap: android.graphics.Bitmap) {
        val dir = File(getExternalFilesDir("screenshots"), "")
        if (!dir.exists()) dir.mkdirs()
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val file = File(dir, "screenshot_${timestamp}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
        }
    }

    private fun createNotification(): Notification {
        val channelId = "screenshot_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Screenshot", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Screenshot Service")
            .setContentText("Capturing screen")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .build()
    }

    companion object {
        const val ACTION_START = "action_start"
        const val ACTION_STOP = "action_stop"
        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_RESULT_DATA = "extra_result_data"
        const val INTERVAL_MS = 10_000L
        const val NOTIFICATION_ID = 1
    }
}
