package com.example.cameraxapp.helper

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object PermissionHelper {

    // Permissions that must be requested at runtime
    val REQUIRED_PERMISSIONS: Array<String> =
        mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ).apply {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }.toTypedArray()

    // Subset of permissions that may require a rationale dialog
    private val DANGEROUS_PERMISSIONS: Array<String> = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )


    fun allPermissionsGranted(context: Context): Boolean {
        return REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun shouldRedirectToSettings(context: Context): Boolean {
        return DANGEROUS_PERMISSIONS.any {
            !ActivityCompat.shouldShowRequestPermissionRationale(context as Activity, it)
        }
    }
    fun isLocationPermissionGranted(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }


    fun getMissingRequiredPermissions(permissions: Map<String, Boolean>): List<String> {
        val denied = mutableListOf<String>()

        if (permissions[Manifest.permission.CAMERA] != true) {
            denied.add("camera")
        }

        if (permissions[Manifest.permission.RECORD_AUDIO] != true) {
            denied.add("audio")
        }

        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] != true) {
            denied.add("precise location")
        }

        return denied
    }
}