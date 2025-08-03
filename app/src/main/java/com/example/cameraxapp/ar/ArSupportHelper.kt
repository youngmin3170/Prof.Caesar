package com.example.cameraxapp.ar

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.cameraxapp.databinding.ActivityArBinding
import com.example.cameraxapp.databinding.ActivityMainBinding
import com.example.cameraxapp.helper.PermissionHelper
import com.google.android.gms.location.LocationServices
import com.google.ar.core.*

import com.google.ar.core.exceptions.*

object ArSupportHelper {

    private var userRequestedInstall = true

    fun maybeEnableArButton(activity: AppCompatActivity, binding: ActivityMainBinding) {
        val arButton = binding.arButton

        // Check ARCore support
        ArCoreApk.getInstance().checkAvailabilityAsync(activity) { availability ->
            if (!availability.isSupported) {
                Log.w("ARCheck", "ARCore not supported on this device")
                arButton.visibility = View.INVISIBLE
                arButton.isEnabled = false
                return@checkAvailabilityAsync
            } else {
                Log.i("ARCheck", "ARCore supported")
            }

            // Check location permission
            if (!PermissionHelper.isLocationPermissionGranted(activity)) {
                Log.w("ARCheck", "Location permission not granted")
                arButton.visibility = View.INVISIBLE
                arButton.isEnabled = false
                return@checkAvailabilityAsync
            }

            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity)

            try {
                // Get last known location
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location == null) {
                        Log.w("ARCheck", "Location is null")
                        arButton.visibility = View.INVISIBLE
                        arButton.isEnabled = false
                        return@addOnSuccessListener
                    }

                    val lat = location.latitude
                    val lon = location.longitude
                    Log.i("ARCheck", "Got location: ($lat, $lon)")

                    // Check VPS availability
                    try {
                        val session = Session(activity)
                        session.checkVpsAvailabilityAsync(lat, lon) { vpsAvailability ->
                            Log.i("ARCheck", "VPS availability: $vpsAvailability")

                            if (vpsAvailability == VpsAvailability.AVAILABLE) {
                                Log.i("ARCheck", "VPS available -> showing AR button")
                                arButton.visibility = View.VISIBLE
                                arButton.isEnabled = true
                            } else {
                                Log.w("ARCheck", "VPS unavailable at this location")
                                arButton.visibility = View.INVISIBLE
                                arButton.isEnabled = false
                            }
                            session.close()
                        }
                    } catch (e: Exception) {
                        Log.e("ARCheck", "VPS check failed: ${e.localizedMessage}")
                        arButton.visibility = View.INVISIBLE
                        arButton.isEnabled = false
                    }
                }
            } catch (e: SecurityException) {
                Log.e("ARCheck", "SecurityException: Location permission not granted at runtime")
                arButton.visibility = View.INVISIBLE
                arButton.isEnabled = false
            }
        }
    }


    fun ensureArCoreInstalled(context: Context): Boolean {
        if (!PermissionHelper.allPermissionsGranted(context)) {
            Toast.makeText(context, "Required permissions not granted", Toast.LENGTH_LONG).show()
            return false
        }

        return try {
            val status = ArCoreApk.getInstance().requestInstall(context as Activity, userRequestedInstall)
            if (status == ArCoreApk.InstallStatus.INSTALLED) {
                true
            } else {
                userRequestedInstall = false
                false
            }
        } catch (e: UnavailableUserDeclinedInstallationException) {
            Toast.makeText(context, "User declined ARCore install", Toast.LENGTH_LONG).show()
            false
        } catch (e: Exception) {
            Toast.makeText(context, "ARCore error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            false
        }
    }

    fun isARCoreSupportedAndUpToDate(activity: Activity): Boolean {
        return when (ArCoreApk.getInstance().checkAvailability(activity)) {
            ArCoreApk.Availability.SUPPORTED_INSTALLED -> true

            ArCoreApk.Availability.SUPPORTED_APK_TOO_OLD,
            ArCoreApk.Availability.SUPPORTED_NOT_INSTALLED -> {
                try {
                    ArCoreApk.getInstance().requestInstall(activity, true) ==
                            ArCoreApk.InstallStatus.INSTALLED
                } catch (e: UnavailableException) {
                    Log.e("ArSupportHelper", "ARCore not installed", e)
                    false
                }
            }

            ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE -> false

            ArCoreApk.Availability.UNKNOWN_CHECKING -> {
                Handler(Looper.getMainLooper()).postDelayed({
                    isARCoreSupportedAndUpToDate(activity)
                }, 200)
                false
            }

            else -> false
        }
    }
}
