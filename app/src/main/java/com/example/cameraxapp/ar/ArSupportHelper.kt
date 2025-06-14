package com.example.cameraxapp.ar


import android.content.Context
import android.view.View
import android.widget.Toast
import com.google.ar.core.ArCoreApk
import com.example.cameraxapp.databinding.ActivityMainBinding
import androidx.appcompat.app.AppCompatActivity
import com.example.cameraxapp.helper.PermissionHelper
import com.google.ar.core.Session
import com.google.ar.core.exceptions.*


object ArSupportHelper {

    var session: Session? = null
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
            Toast.makeText(context, "Camera permission not granted", Toast.LENGTH_LONG).show()
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
}