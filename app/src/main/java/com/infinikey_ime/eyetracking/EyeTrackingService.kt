package com.infinikey_ime.eyetracking

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import com.infinikey_ime.R
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * Foreground Service for Eye Tracking & Gaze Estimation.
 * 
 * Features:
 * - Persistent notification in status bar with an active blinking indicator light.
 * - Background camera frame capture using Camera2 API (Front Camera + ImageReader).
 * - Fully decoupled architecture so it can be extracted to a standalone app.
 */
class EyeTrackingService : Service() {

    companion object {
        private const val TAG = "EyeTrackingService"
        const val CHANNEL_ID = "eyetracking_service_channel"
        const val NOTIFICATION_ID = 4001

        const val ACTION_START = "com.infinikey_ime.action.START_EYE_TRACKING"
        const val ACTION_STOP = "com.infinikey_ime.action.STOP_EYE_TRACKING"
        const val ACTION_CALIBRATE = "com.infinikey_ime.action.CALIBRATE_EYE_TRACKING"

        @Volatile
        var isRunning: Boolean = false
            private set

        fun startService(context: Context) {
            val intent = Intent(context, EyeTrackingService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, EyeTrackingService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun startCalibration(context: Context) {
            val intent = Intent(context, EyeTrackingService::class.java).apply {
                action = ACTION_CALIBRATE
            }
            context.startService(intent)
        }
    }

    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var imageReader: ImageReader? = null
    private var cameraThread: HandlerThread? = null
    private var cameraHandler: Handler? = null

    // Blinking Light / Status Indicator animation
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isBlinkState = false
    private var frameCounter = AtomicLong(0)
    private val isCapturing = AtomicBoolean(false)

    private val statusBlinkRunnable = object : Runnable {
        override fun run() {
            if (!isRunning) return
            isBlinkState = !isBlinkState
            updateNotification()
            mainHandler.postDelayed(this, 1000)
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "EyeTrackingService created.")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        val action = intent?.action
        if (action == ACTION_STOP) {
            Log.d(TAG, "Stopping EyeTrackingService via action.")
            stopForegroundAndService()
            return START_NOT_STICKY
        }

        if (action == ACTION_CALIBRATE) {
            Log.d(TAG, "Starting eye tracking calibration.")
            mediaPipeGazeEngine?.startCalibration()
            return START_STICKY
        }

        if (!isRunning) {
            isRunning = true
            startForeground(NOTIFICATION_ID, buildNotification())
            mainHandler.post(statusBlinkRunnable)

            // Initialize and show the floating mouse cursor overlay
            mainHandler.post {
                initCursorOverlay()
            }

            if (CameraPermissionActivity.hasCameraPermission(this)) {
                startCameraBackgroundThread()
                openFrontCamera()
            } else {
                Log.w(TAG, "Camera permission not granted; cannot open camera.")
            }
        }

        return START_STICKY
    }

    private var gazeCursorOverlay: GazeCursorOverlay? = null
    private var mediaPipeGazeEngine: MediaPipeGazeEngine? = null

    private fun initCursorOverlay() {
        if (gazeCursorOverlay == null) {
            gazeCursorOverlay = GazeCursorOverlay(this)
        }
        gazeCursorOverlay?.show()
        val metrics = resources.displayMetrics
        gazeCursorOverlay?.updatePosition(metrics.widthPixels / 2, metrics.heightPixels / 2)

        if (mediaPipeGazeEngine == null) {
            mediaPipeGazeEngine = MediaPipeGazeEngine(
                context = this,
                screenWidth = metrics.widthPixels,
                screenHeight = metrics.heightPixels,
                onGazeUpdated = { x, y ->
                    updateCursorPosition(x.toInt(), y.toInt())
                },
                onLongBlinkClick = { _, _ ->
                    triggerCursorClick()
                },
                onCalibrationProgress = { step, x, y ->
                    mainHandler.post {
                        updateCursorPosition(x, y)
                    }
                },
                onCalibrationFinished = { metrics ->
                    mainHandler.post {
                        val report = metrics.toFormattedReport()
                        Log.i(TAG, "Calibration Evaluation Report:\n$report")

                        val intent = Intent(this, CalibrationReportActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            putExtra("REPORT_TEXT", report)
                            putExtra("IS_SUCCESS", metrics.isSuccessful)
                        }
                        startActivity(intent)

                        if (!metrics.isSuccessful) {
                            stopSelf()
                        }
                    }
                }

            )
        }
    }



    fun updateCursorPosition(x: Int, y: Int) {
        mainHandler.post {
            gazeCursorOverlay?.updatePosition(x, y)
        }
    }

    fun triggerCursorClick() {
        mainHandler.post {
            gazeCursorOverlay?.triggerClickAnimation()
        }
    }



    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Eye Tracking Gaze Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows ongoing status and blinking active indicator for eye tracking."
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val stopIntent = Intent(this, EyeTrackingService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val indicatorSymbol = if (isBlinkState) "🟢 ACTIVE" else "🟢 •  •  •"
        val statusMessage = "Eye Tracking Running $indicatorSymbol"
        val fps = frameCounter.getAndResetFps()
        val subText = if (isCapturing.get()) "Camera Live ($fps fps)" else "Initializing Camera..."

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(statusMessage)
            .setContentText(subText)
            .setSmallIcon(R.drawable.ic_eye_tracking)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Stop Service",
                stopPendingIntent
            )
            .build()
    }

    private fun updateNotification() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun startCameraBackgroundThread() {
        cameraThread = HandlerThread("EyeTrackingCameraThread").also { it.start() }
        cameraHandler = Handler(cameraThread!!.looper)
    }

    @SuppressLint("MissingPermission")
    private fun openFrontCamera() {
        val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            val frontCameraId = cameraManager.cameraIdList.firstOrNull { id ->
                val char = cameraManager.getCameraCharacteristics(id)
                val facing = char.get(CameraCharacteristics.LENS_FACING)
                facing == CameraCharacteristics.LENS_FACING_FRONT
            } ?: cameraManager.cameraIdList.firstOrNull()

            if (frontCameraId == null) {
                Log.e(TAG, "No front camera found on device.")
                return
            }

            val characteristics = cameraManager.getCameraCharacteristics(frontCameraId)
            val orientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 270
            mediaPipeGazeEngine?.sensorOrientation = orientation


            // Create ImageReader for YUV frame processing (640x480 resolution for low latency)
            imageReader = ImageReader.newInstance(640, 480, ImageFormat.YUV_420_888, 3).apply {

                setOnImageAvailableListener({ reader ->
                    val image = reader.acquireNextImage()
                    if (image != null) {
                        frameCounter.incrementAndGet()
                        isCapturing.set(true)
                        mediaPipeGazeEngine?.processImage(image)
                        image.close()
                    }
                }, cameraHandler)
            }


            cameraManager.openCamera(frontCameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraDevice = camera
                    createCaptureSession()
                }

                override fun onDisconnected(camera: CameraDevice) {
                    camera.close()
                    cameraDevice = null
                    isCapturing.set(false)
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    Log.e(TAG, "Camera device error: $error")
                    camera.close()
                    cameraDevice = null
                    isCapturing.set(false)
                }
            }, cameraHandler)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to open camera: ${e.message}", e)
        }
    }

    private fun createCaptureSession() {
        val camera = cameraDevice ?: return
        val reader = imageReader ?: return
        try {
            val surface = reader.surface
            val requestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                addTarget(surface)
            }

            camera.createCaptureSession(
                listOf(surface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        captureSession = session
                        try {
                            session.setRepeatingRequest(requestBuilder.build(), null, cameraHandler)
                            Log.d(TAG, "Camera capture session configured successfully.")
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to start repeating capture request: ${e.message}", e)
                        }
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Log.e(TAG, "Camera capture session configuration failed.")
                        isCapturing.set(false)
                    }
                },
                cameraHandler
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error creating capture session: ${e.message}", e)
        }
    }

    private fun stopForegroundAndService() {
        isRunning = false
        mainHandler.removeCallbacks(statusBlinkRunnable)

        mainHandler.post {
            gazeCursorOverlay?.hide()
            gazeCursorOverlay = null
            mediaPipeGazeEngine?.close()
            mediaPipeGazeEngine = null
        }


        try {
            captureSession?.close()
            captureSession = null
            cameraDevice?.close()
            cameraDevice = null
            imageReader?.close()
            imageReader = null
            cameraThread?.quitSafely()
            cameraThread = null
        } catch (e: Exception) {
            Log.e(TAG, "Error shutting down camera: ${e.message}", e)
        }


        stopForeground(true)
        stopSelf()
    }

    override fun onDestroy() {

        stopForegroundAndService()
        super.onDestroy()
        Log.d(TAG, "EyeTrackingService destroyed.")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

// Helper to calculate frame rate
private var lastFpsTimestamp = System.currentTimeMillis()
private var lastFrameCount = 0L
private var currentFps = 0

private fun AtomicLong.getAndResetFps(): Int {
    val now = System.currentTimeMillis()
    val elapsed = now - lastFpsTimestamp
    if (elapsed >= 1000) {
        val count = this.get()
        val deltaFrames = count - lastFrameCount
        currentFps = ((deltaFrames * 1000) / elapsed).toInt()
        lastFrameCount = count
        lastFpsTimestamp = now
    }
    return currentFps
}
