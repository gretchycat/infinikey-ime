package com.infinikey_ime.eyetracking

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Transparent proxy activity to request CAMERA and POST_NOTIFICATIONS runtime permissions.
 * Designed to be modular so it can be extracted to a standalone application later.
 */
class CameraPermissionActivity : Activity() {

    companion object {
        const val REQUEST_CODE_CAMERA_PERMISSIONS = 3001
        private var onPermissionResultListener: ((Boolean) -> Unit)? = null

        fun hasCameraPermission(context: Context): Boolean {
            val cameraGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED

            val notificationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }

            return cameraGranted && notificationGranted
        }

        fun requestPermissions(context: Context, callback: (Boolean) -> Unit) {
            if (hasCameraPermission(context)) {
                callback(true)
                return
            }
            onPermissionResultListener = callback
            val intent = Intent(context, CameraPermissionActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (hasCameraPermission(this)) {
            onPermissionResultListener?.invoke(true)
            onPermissionResultListener = null
            finish()
            return
        }

        val permissionsToRequest = mutableListOf(Manifest.permission.CAMERA)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        ActivityCompat.requestPermissions(
            this,
            permissionsToRequest.toTypedArray(),
            REQUEST_CODE_CAMERA_PERMISSIONS
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_CAMERA_PERMISSIONS) {
            val granted = hasCameraPermission(this)
            onPermissionResultListener?.invoke(granted)
            onPermissionResultListener = null
        }
        finish()
    }
}
