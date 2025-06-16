package com.example.cameraxapp.ar


import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import com.google.ar.core.ArCoreApk
import com.example.cameraxapp.databinding.ActivityMainBinding
import androidx.appcompat.app.AppCompatActivity
import com.example.cameraxapp.helper.PermissionHelper
import com.google.ar.core.Session
import com.google.ar.core.exceptions.*


object ArSupportHelper {

    private var session: Session? = null
    private var userRequestedInstall = true


    fun maybeEnableArButton(activity: AppCompatActivity, binding: ActivityMainBinding) {
        val arButton = binding.arButton

        ArCoreApk.getInstance().checkAvailabilityAsync(activity) { availability ->
            if (availability.isSupported) {
                arButton.visibility = View.VISIBLE
                arButton.isEnabled = true
            } else {
                arButton.visibility = View.INVISIBLE
                arButton.isEnabled = false
            }
        }
    }

    fun ensureArCoreInstalled(context: Context): Boolean {
        if (!PermissionHelper.allPermissionsGranted(context)) {
            Toast.makeText(context, "Camera or audio permission not granted", Toast.LENGTH_LONG).show()
            return false
        }

        try {
            if (session == null) {
                when (ArCoreApk.getInstance().requestInstall(context as AppCompatActivity, userRequestedInstall)) {
                    ArCoreApk.InstallStatus.INSTALLED -> {
                        session = Session(context)
                    }
                    ArCoreApk.InstallStatus.INSTALL_REQUESTED -> {
                        userRequestedInstall = false
                        return false
                    }
                }
            }
        } catch (e: UnavailableUserDeclinedInstallationException) {
            Toast.makeText(context, "User declined ARCore install", Toast.LENGTH_LONG).show()
            return false
        } catch (e: Exception) {
            Toast.makeText(context, "ARCore failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            return false
        }
        return true
    }

    // Helper function to verify that ARCore is installed and using the current version.
    fun isARCoreSupportedAndUpToDate(activity: Activity): Boolean {
        when (ArCoreApk.getInstance().checkAvailability(activity)) {
            ArCoreApk.Availability.SUPPORTED_INSTALLED -> return true

            ArCoreApk.Availability.SUPPORTED_APK_TOO_OLD,
            ArCoreApk.Availability.SUPPORTED_NOT_INSTALLED -> {
                return try {
                    when (ArCoreApk.getInstance().requestInstall(activity, true)) {
                        ArCoreApk.InstallStatus.INSTALLED -> true
                        ArCoreApk.InstallStatus.INSTALL_REQUESTED -> {
                            Log.i("ArSupportHelper", "ARCore installation requested.")
                            false
                        }
                    }
                } catch (e: UnavailableException) {
                    Log.e("ArSupportHelper", "ARCore not installed", e)
                    false
                }
            }

            ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE -> {
                Log.e("ArSupportHelper", "Device not AR-capable.")
                return false
            }

            ArCoreApk.Availability.UNKNOWN_CHECKING -> {
                Handler(Looper.getMainLooper()).postDelayed({
                    isARCoreSupportedAndUpToDate(activity)
                }, 200)
                return false
            }
            ArCoreApk.Availability.UNKNOWN_ERROR,
            ArCoreApk.Availability.UNKNOWN_TIMED_OUT -> {
                Log.w("ArSupportHelper", "ARCore availability unknown or checking failed.")
                return false
            }
        }
    }

}