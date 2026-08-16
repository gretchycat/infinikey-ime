package com.infinikey_ime.eyetracking

import android.content.Context
import com.infinikey_ime.util.OverlayPermissionUtil

/**
 * Controller object to manage starting/stopping and calibrating the Eye Tracking Service.
 */
object EyeTrackingManager {

    fun isServiceRunning(): Boolean = EyeTrackingService.isRunning

    fun start(context: Context, onPermissionDenied: (() -> Unit)? = null) {
        if (!OverlayPermissionUtil.hasOverlayPermission(context)) {
            OverlayPermissionUtil.requestOverlayPermission(context)
            onPermissionDenied?.invoke()
            return
        }

        if (CameraPermissionActivity.hasCameraPermission(context)) {
            EyeTrackingService.startService(context)
        } else {
            CameraPermissionActivity.requestPermissions(context) { granted ->
                if (granted) {
                    EyeTrackingService.startService(context)
                } else {
                    onPermissionDenied?.invoke()
                }
            }
        }
    }

    fun stop(context: Context) {
        EyeTrackingService.stopService(context)
    }

    fun toggle(context: Context, onPermissionDenied: (() -> Unit)? = null) {
        if (isServiceRunning()) {
            stop(context)
        } else {
            start(context, onPermissionDenied)
        }
    }

    fun startCalibration(context: Context) {
        if (!isServiceRunning()) {
            start(context)
        }
        EyeTrackingService.startCalibration(context)
    }
}
