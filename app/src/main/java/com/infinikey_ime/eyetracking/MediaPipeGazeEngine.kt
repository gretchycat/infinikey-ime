package com.infinikey_ime.eyetracking

import android.content.Context
import android.graphics.*
import android.media.Image
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import java.io.ByteArrayOutputStream
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

enum class CalibrationStep(val title: String) {
    NONE("Idle"),
    SPIRAL_IN_PROGRESS("Continuous Aspect-Ratio Spiral Calibration 🌀"),
    COMPLETE("Calibration Complete! 🎉"),
    FAILED("Calibration Accuracy Low ⚠️")
}

/**
 * Biometric Gaze Calibration Metrics Data Structure.
 */
data class GazeCalibrationMetrics(
    val meanErrorPx: Float,
    val maxErrorPx: Float,
    val angularErrorDeg: Float,
    val jitterRmsPx: Float,
    val totalSamples: Int,
    val isSuccessful: Boolean
) {
    fun toFormattedReport(): String {
        return StringBuilder().apply {
            append(if (isSuccessful) "🎉 Calibration Verified!\n" else "❌ Calibration Failed!\n")
            append(String.format("• Mean Spatial Error: %.1f px (Threshold: 180.0 px)\n", meanErrorPx))
            append(String.format("• Angular Deviation: %.1f°\n", angularErrorDeg))
            append(String.format("• Micro-Jitter RMS: %.1f px\n", jitterRmsPx))
            append(String.format("• Peak Offset Error: %.1f px\n", maxErrorPx))
            append(String.format("• Evaluated Frames: %d", totalSamples))
            if (!isSuccessful) append("\nService stopped due to low accuracy.")
        }.toString()
    }
}

/**
 * MediaPipe Gaze Engine with Fixed NV21-to-ARGB Decoder & Correct Matrix Transform.
 */
class MediaPipeGazeEngine(
    private val context: Context,
    private val screenWidth: Int,
    private val screenHeight: Int,
    private val onGazeUpdated: (x: Float, y: Float) -> Unit,
    private val onLongBlinkClick: (x: Float, y: Float) -> Unit,
    private val onCalibrationProgress: (step: CalibrationStep, x: Int, y: Int) -> Unit,
    private val onCalibrationFinished: (metrics: GazeCalibrationMetrics) -> Unit
) {

    companion object {
        private const val TAG = "MediaPipeGazeEngine"

        private const val RIGHT_IRIS_CENTER = 468
        private const val LEFT_IRIS_CENTER = 473

        private const val RIGHT_EYE_INNER = 133
        private const val RIGHT_EYE_OUTER = 33
        private const val RIGHT_EYE_TOP = 159
        private const val RIGHT_EYE_BOTTOM = 145

        private const val LEFT_EYE_INNER = 362
        private const val LEFT_EYE_OUTER = 263
        private const val LEFT_EYE_TOP = 386
        private const val LEFT_EYE_BOTTOM = 374

        private const val SPIRAL_LOOPS = 4f
        private const val TOTAL_SPIRAL_DURATION_MS = 16000L
        private const val ERROR_THRESHOLD_PX = 180f
    }

    var sensorOrientation: Int = 270

    private var faceLandmarker: FaceLandmarker? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private var polyCx = floatArrayOf(screenWidth / 2f, 14f * screenWidth, 0f, 0f, 0f, 0f)
    private var polyCy = floatArrayOf(screenHeight / 2f, 0f, 14f * screenHeight, 0f, 0f, 0f)
    private var isPolynomialCalibrated = false

    var isCalibrating = false
        private set
    private var spiralStartTime = 0L
    private var currentTargetX = screenWidth / 2f
    private var currentTargetY = screenHeight / 2f

    private val calibDxSamples = mutableListOf<Float>()
    private val calibDySamples = mutableListOf<Float>()
    private val calibTargetXSamples = mutableListOf<Float>()
    private val calibTargetYSamples = mutableListOf<Float>()

    private val marginPx = 90f
    private val maxRadiusX = (screenWidth / 2f) - marginPx
    private val maxRadiusY = (screenHeight / 2f) - marginPx

    private val medianWindowDx = FloatArray(5)
    private val medianWindowDy = FloatArray(5)
    private var medianIdx = 0

    private val spiralTickerRunnable = object : Runnable {
        override fun run() {
            if (!isCalibrating) return
            val elapsed = SystemClock.elapsedRealtime() - spiralStartTime

            if (elapsed >= TOTAL_SPIRAL_DURATION_MS) {
                finishSpiralCalibration()
                return
            }

            val progress = (elapsed.toFloat() / TOTAL_SPIRAL_DURATION_MS).coerceIn(0f, 1f)
            val decay = 1.0f - progress // Outside -> Inside

            val theta = progress * (SPIRAL_LOOPS * 2f * Math.PI.toFloat())
            val rx = decay * maxRadiusX
            val ry = decay * maxRadiusY

            val cx = screenWidth / 2f
            val cy = screenHeight / 2f

            currentTargetX = cx + rx * cos(theta)
            currentTargetY = cy + ry * sin(theta)

            onCalibrationProgress(CalibrationStep.SPIRAL_IN_PROGRESS, currentTargetX.toInt(), currentTargetY.toInt())
            mainHandler.postDelayed(this, 16)
        }
    }

    private var blinkStartTime = 0L
    private var isBlinkingState = false
    private var lastPreBlinkX = screenWidth / 2f
    private var lastPreBlinkY = screenHeight / 2f

    private val ringBufferX = FloatArray(10)
    private val ringBufferY = FloatArray(10)
    private var ringBufferIndex = 0

    init {
        initMediaPipe()
    }

    private fun initMediaPipe() {
        try {
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath("face_landmarker.task")
                .build()

            val options = FaceLandmarker.FaceLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setMinFaceDetectionConfidence(0.5f)
                .setMinFacePresenceConfidence(0.5f)
                .setMinTrackingConfidence(0.5f)
                .setNumFaces(1)
                .setOutputFaceBlendshapes(true)
                .setRunningMode(RunningMode.IMAGE)
                .build()

            faceLandmarker = FaceLandmarker.createFromOptions(context, options)
            Log.d(TAG, "MediaPipe FaceLandmarker initialized.")
        } catch (e: Exception) {
            Log.e(TAG, "MediaPipe init error: ${e.message}", e)
        }
    }

    fun startCalibration() {
        isCalibrating = true
        calibDxSamples.clear()
        calibDySamples.clear()
        calibTargetXSamples.clear()
        calibTargetYSamples.clear()

        spiralStartTime = SystemClock.elapsedRealtime()
        currentTargetX = (screenWidth / 2f) + maxRadiusX
        currentTargetY = screenHeight / 2f

        mainHandler.post(spiralTickerRunnable)
        Log.d(TAG, "Aspect-Ratio Elliptical Spiral Calibration started.")
    }

    private fun finishSpiralCalibration() {
        isCalibrating = false
        mainHandler.removeCallbacks(spiralTickerRunnable)

        solvePolynomialRegression(calibDxSamples, calibDySamples, calibTargetXSamples, calibTargetYSamples)

        val n = calibDxSamples.size
        if (n < 5) {
            val failedMetrics = GazeCalibrationMetrics(999f, 999f, 180f, 999f, 0, false)
            onCalibrationProgress(CalibrationStep.FAILED, screenWidth / 2, screenHeight / 2)
            onCalibrationFinished(failedMetrics)
            return
        }

        val cx = screenWidth / 2f
        val cy = screenHeight / 2f

        val spatialErrors = FloatArray(n)
        val angularErrors = FloatArray(n)
        val deltaSteps = FloatArray(n - 1)

        var prevPredX = 0f
        var prevPredY = 0f

        for (i in 0 until n) {
            val dx = calibDxSamples[i]
            val dy = calibDySamples[i]
            val targetX = calibTargetXSamples[i]
            val targetY = calibTargetYSamples[i]

            val predX: Float
            val predY: Float
            if (isPolynomialCalibrated) {
                val dx2 = dx * dx
                val dy2 = dy * dy
                val dxdy = dx * dy
                predX = (polyCx[0] + polyCx[1] * dx + polyCx[2] * dy + polyCx[3] * dx2 + polyCx[4] * dy2 + polyCx[5] * dxdy).coerceIn(0f, screenWidth.toFloat())
                predY = (polyCy[0] + polyCy[1] * dx + polyCy[2] * dy + polyCy[3] * dx2 + polyCy[4] * dy2 + polyCy[5] * dxdy).coerceIn(0f, screenHeight.toFloat())
            } else {
                predX = (cx + dx * 14.0f * screenWidth).coerceIn(0f, screenWidth.toFloat())
                predY = (cy + dy * 14.0f * screenHeight).coerceIn(0f, screenHeight.toFloat())
            }

            val errPx = hypot((predX - targetX).toDouble(), (predY - targetY).toDouble()).toFloat()
            spatialErrors[i] = errPx

            val targetAngleRad = Math.atan2((targetY - cy).toDouble(), (targetX - cx).toDouble())
            val predAngleRad = Math.atan2((predY - cy).toDouble(), (predX - cx).toDouble())
            var angleDiffDeg = Math.toDegrees(abs(predAngleRad - targetAngleRad)).toFloat()
            if (angleDiffDeg > 180f) angleDiffDeg = 360f - angleDiffDeg
            angularErrors[i] = angleDiffDeg

            if (i > 0) {
                deltaSteps[i - 1] = hypot((predX - prevPredX).toDouble(), (predY - prevPredY).toDouble()).toFloat()
            }
            prevPredX = predX
            prevPredY = predY
        }

        val meanError = spatialErrors.average().toFloat()
        val maxError = spatialErrors.maxOrNull() ?: meanError
        val meanAngularError = angularErrors.average().toFloat()

        val avgDelta = deltaSteps.average().toFloat()
        var jitterVarSum = 0.0
        for (delta in deltaSteps) {
            val diff = delta - avgDelta
            jitterVarSum += (diff * diff)
        }
        val jitterRms = Math.sqrt(jitterVarSum / deltaSteps.size.coerceAtLeast(1)).toFloat()

        val isSuccessful = meanError <= ERROR_THRESHOLD_PX

        val metrics = GazeCalibrationMetrics(
            meanErrorPx = meanError,
            maxErrorPx = maxError,
            angularErrorDeg = meanAngularError,
            jitterRmsPx = jitterRms,
            totalSamples = n,
            isSuccessful = isSuccessful
        )

        val step = if (isSuccessful) CalibrationStep.COMPLETE else CalibrationStep.FAILED
        onCalibrationProgress(step, screenWidth / 2, screenHeight / 2)
        onCalibrationFinished(metrics)

        Log.d(TAG, "Spiral Calibration Evaluated Metrics:\n${metrics.toFormattedReport()}")
    }

    fun processImage(image: Image) {
        val bitmap = yuv420ToBitmap(image) ?: return
        val mpImage = BitmapImageBuilder(bitmap).build()

        try {
            val result = faceLandmarker?.detect(mpImage)
            if (result != null && result.faceLandmarks().isNotEmpty()) {
                processLandmarks(result)
            } else {
                Log.d(TAG, "No face detected in camera frame.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error detecting face: ${e.message}")
        } finally {
            bitmap.recycle()
        }
    }

    private var prevAvgDx = 0f
    private var prevAvgDy = 0f
    private var hasPrevGaze = false

    private fun processLandmarks(result: FaceLandmarkerResult) {
        val landmarks = result.faceLandmarks()[0]
        if (landmarks.size < 478) return

        val rightIris = landmarks[RIGHT_IRIS_CENTER]
        val rightInner = landmarks[RIGHT_EYE_INNER]
        val rightOuter = landmarks[RIGHT_EYE_OUTER]
        val rightTop = landmarks[RIGHT_EYE_TOP]
        val rightBottom = landmarks[RIGHT_EYE_BOTTOM]

        val leftIris = landmarks[LEFT_IRIS_CENTER]
        val leftInner = landmarks[LEFT_EYE_INNER]
        val leftOuter = landmarks[LEFT_EYE_OUTER]
        val leftTop = landmarks[LEFT_EYE_TOP]
        val leftBottom = landmarks[LEFT_EYE_BOTTOM]

        val rightCenterX = (rightInner.x() + rightOuter.x()) / 2f
        val rightCenterY = (rightTop.y() + rightBottom.y()) / 2f

        val leftCenterX = (leftInner.x() + leftOuter.x()) / 2f
        val leftCenterY = (leftTop.y() + leftBottom.y()) / 2f

        val faceWidth = hypot(
            (leftOuter.x() - rightOuter.x()).toDouble(),
            (leftOuter.y() - rightOuter.y()).toDouble()
        ).toFloat().coerceAtLeast(0.05f)

        val rightDx = (rightIris.x() - rightCenterX) / faceWidth
        val rightDy = (rightIris.y() - rightCenterY) / faceWidth

        val leftDx = (leftIris.x() - leftCenterX) / faceWidth
        val leftDy = (leftIris.y() - leftCenterY) / faceWidth

        val rawDx = (rightDx + leftDx) / 2f
        val rawDy = (rightDy + leftDy) / 2f

        // 5-Sample Moving Median Filter to eliminate micro-jitter noise spikes
        medianWindowDx[medianIdx] = rawDx
        medianWindowDy[medianIdx] = rawDy
        medianIdx = (medianIdx + 1) % medianWindowDx.size

        val sortedDx = medianWindowDx.clone().apply { sort() }
        val sortedDy = medianWindowDy.clone().apply { sort() }

        var filteredDx = sortedDx[2]
        var filteredDy = sortedDy[2]

        if (!hasPrevGaze) {
            prevAvgDx = filteredDx
            prevAvgDy = filteredDy
            hasPrevGaze = true
        } else {
            val jumpDist = hypot((filteredDx - prevAvgDx).toDouble(), (filteredDy - prevAvgDy).toDouble()).toFloat()
            if (jumpDist > 0.08f) {
                filteredDx = prevAvgDx
                filteredDy = prevAvgDy
            } else {
                filteredDx = prevAvgDx + 0.35f * (filteredDx - prevAvgDx)
                filteredDy = prevAvgDy + 0.35f * (filteredDy - prevAvgDy)
                prevAvgDx = filteredDx
                prevAvgDy = filteredDy
            }
        }

        val avgDx = filteredDx
        val avgDy = filteredDy

        val predX: Float
        val predY: Float

        if (isPolynomialCalibrated) {
            val dx = avgDx
            val dy = avgDy
            val dx2 = dx * dx
            val dy2 = dy * dy
            val dxdy = dx * dy

            predX = polyCx[0] + polyCx[1] * dx + polyCx[2] * dy + polyCx[3] * dx2 + polyCx[4] * dy2 + polyCx[5] * dxdy
            predY = polyCy[0] + polyCy[1] * dx + polyCy[2] * dy + polyCy[3] * dx2 + polyCy[4] * dy2 + polyCy[5] * dxdy
        } else {
            predX = (screenWidth / 2f) + (avgDx * 14.0f * screenWidth)
            predY = (screenHeight / 2f) + (avgDy * 14.0f * screenHeight)
        }

        val clampedX = predX.coerceIn(0f, screenWidth.toFloat())
        val clampedY = predY.coerceIn(0f, screenHeight.toFloat())

        if (isCalibrating) {
            val targetX = currentTargetX
            val targetY = currentTargetY

            calibDxSamples.add(avgDx)
            calibDySamples.add(avgDy)
            calibTargetXSamples.add(targetX)
            calibTargetYSamples.add(targetY)

            if (calibDxSamples.size % 25 == 0 && calibDxSamples.size >= 30) {
                solvePolynomialRegression(calibDxSamples, calibDySamples, calibTargetXSamples, calibTargetYSamples)
            }

            onGazeUpdated(targetX, targetY)
            return
        }

        ringBufferX[ringBufferIndex] = clampedX
        ringBufferY[ringBufferIndex] = clampedY
        ringBufferIndex = (ringBufferIndex + 1) % ringBufferX.size

        var isEyesClosed = false
        val blendshapesOptional = result.faceBlendshapes()
        if (blendshapesOptional.isPresent && blendshapesOptional.get().isNotEmpty()) {
            val blendshapeCategories = blendshapesOptional.get()[0]
            var blinkLeft = 0f
            var blinkRight = 0f
            for (category in blendshapeCategories) {
                if (category.categoryName() == "eyeBlinkLeft") blinkLeft = category.score()
                if (category.categoryName() == "eyeBlinkRight") blinkRight = category.score()
            }
            isEyesClosed = (blinkLeft > 0.45f && blinkRight > 0.45f)
        }

        val now = SystemClock.elapsedRealtime()

        if (isEyesClosed) {
            if (!isBlinkingState) {
                val lockedIdx = (ringBufferIndex + 1) % ringBufferX.size
                lastPreBlinkX = ringBufferX[lockedIdx]
                lastPreBlinkY = ringBufferY[lockedIdx]
                blinkStartTime = now
                isBlinkingState = true
            }
        } else {
            if (isBlinkingState) {
                val blinkDuration = now - blinkStartTime
                isBlinkingState = false

                if (blinkDuration in 320..750) {
                    Log.d(TAG, "Long Blink detected ($blinkDuration ms)")
                    onLongBlinkClick(lastPreBlinkX, lastPreBlinkY)
                }
            }
        }

        onGazeUpdated(clampedX, clampedY)
    }

    private fun solvePolynomialRegression(
        pointsDx: List<Float>,
        pointsDy: List<Float>,
        targetsX: List<Float>,
        targetsY: List<Float>
    ) {
        val n = pointsDx.size
        if (n < 6) return

        val A = Array(n) { i ->
            val dx = pointsDx[i].toDouble()
            val dy = pointsDy[i].toDouble()
            doubleArrayOf(1.0, dx, dy, dx * dx, dy * dy, dx * dy)
        }

        val AtA = Array(6) { DoubleArray(6) }
        val AtX = DoubleArray(6)
        val AtY = DoubleArray(6)

        for (i in 0 until n) {
            for (r in 0 until 6) {
                AtX[r] += A[i][r] * targetsX[i]
                AtY[r] += A[i][r] * targetsY[i]
                for (c in 0 until 6) {
                    AtA[r][c] += A[i][r] * A[i][c]
                }
            }
        }

        val cxD = solveLinearSystem(AtA, AtX)
        val cyD = solveLinearSystem(AtA, AtY)

        if (cxD != null && cyD != null) {
            for (i in 0 until 6) {
                polyCx[i] = cxD[i].toFloat()
                polyCy[i] = cyD[i].toFloat()
            }
            isPolynomialCalibrated = true
        }
    }

    private fun solveLinearSystem(A_orig: Array<DoubleArray>, B_orig: DoubleArray): DoubleArray? {
        val n = B_orig.size
        val A = Array(n) { i -> A_orig[i].clone() }
        val B = B_orig.clone()

        for (i in 0 until n) {
            var maxRow = i
            for (k in i + 1 until n) {
                if (abs(A[k][i]) > abs(A[maxRow][i])) {
                    maxRow = k
                }
            }
            val tempA = A[i]; A[i] = A[maxRow]; A[maxRow] = tempA
            val tempB = B[i]; B[i] = B[maxRow]; B[maxRow] = tempB

            if (abs(A[i][i]) < 1e-12) return null

            for (k in i + 1 until n) {
                val factor = A[k][i] / A[i][i]
                B[k] -= factor * B[i]
                for (j in i until n) {
                    A[k][j] -= factor * A[i][j]
                }
            }
        }

        val x = DoubleArray(n)
        for (i in n - 1 downTo 0) {
            var sum = 0.0
            for (j in i + 1 until n) {
                sum += A[i][j] * x[j]
            }
            x[i] = (B[i] - sum) / A[i][i]
        }
        return x
    }

    /**
     * High-speed YUV_420_888 to ARGB Converter with robust NV21 indexing and matrix ordering.
     */
    private fun yuv420ToBitmap(image: Image): Bitmap? {
        return try {
            val yPlane = image.planes[0]
            val uPlane = image.planes[1]
            val vPlane = image.planes[2]

            val yBuffer = yPlane.buffer
            val uBuffer = uPlane.buffer
            val vBuffer = vPlane.buffer

            val width = image.width
            val height = image.height

            val nv21 = ByteArray(width * height * 3 / 2)

            val yRowStride = yPlane.rowStride
            var nv21Index = 0

            if (yRowStride == width) {
                yBuffer.get(nv21, 0, width * height)
                nv21Index = width * height
            } else {
                for (row in 0 until height) {
                    yBuffer.position(row * yRowStride)
                    yBuffer.get(nv21, nv21Index, width)
                    nv21Index += width
                }
            }

            val vRowStride = vPlane.rowStride
            val vPixelStride = vPlane.pixelStride
            val uPos = uBuffer.position()
            val vPos = vBuffer.position()

            val uvHeight = height / 2
            val uvWidth = width / 2

            for (row in 0 until uvHeight) {
                for (col in 0 until uvWidth) {
                    val vIdx = row * vRowStride + col * vPixelStride
                    val uIdx = row * uPlane.rowStride + col * uPlane.pixelStride

                    val vByte = vBuffer.get(vPos + vIdx)
                    val uByte = uBuffer.get(uPos + uIdx)

                    nv21[nv21Index++] = vByte
                    nv21[nv21Index++] = uByte
                }
            }

            // Direct Fast NV21 -> ARGB Conversion with bounded indexing
            val argb = IntArray(width * height)
            val frameSize = width * height

            for (y in 0 until height) {
                val yRowOffset = y * width
                val uvRowOffset = frameSize + (y shr 1) * width

                for (x in 0 until width) {
                    val yValue = (nv21[yRowOffset + x].toInt() and 0xff) - 16

                    val uvIndex = uvRowOffset + (x shr 1 shl 1)
                    val vValue = (nv21[uvIndex].toInt() and 0xff) - 128
                    val uValue = (nv21[uvIndex + 1].toInt() and 0xff) - 128

                    val y1192 = 1192 * yValue.coerceAtLeast(0)
                    var r = (y1192 + 1634 * vValue)
                    var g = (y1192 - 833 * vValue - 400 * uValue)
                    var b = (y1192 + 2066 * uValue)

                    r = r.coerceIn(0, 262143) shr 10
                    g = g.coerceIn(0, 262143) shr 10
                    b = b.coerceIn(0, 262143) shr 10

                    argb[yRowOffset + x] = (0xff shl 24) or (r shl 16) or (g shl 8) or b
                }
            }

            val rawBitmap = Bitmap.createBitmap(argb, width, height, Bitmap.Config.ARGB_8888)

            val matrix = Matrix().apply {
                postScale(-1f, 1f) // Mirror horizontal FIRST in landscape space
                postRotate(sensorOrientation.toFloat()) // THEN rotate upright
            }

            Bitmap.createBitmap(rawBitmap, 0, 0, width, height, matrix, true).also {
                if (it != rawBitmap) rawBitmap.recycle()
            }
        } catch (e: Exception) {
            Log.e(TAG, "yuv420ToBitmap error: ${e.message}", e)
            null
        }
    }

    fun close() {
        try {
            mainHandler.removeCallbacks(spiralTickerRunnable)
            faceLandmarker?.close()
            faceLandmarker = null
        } catch (e: Exception) {
            Log.e(TAG, "MediaPipe close error: ${e.message}")
        }
    }
}
