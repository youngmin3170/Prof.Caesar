package com.example.cameraxapp.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.cameraxapp.databinding.ActivityMainBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.example.cameraxapp.R
import com.example.cameraxapp.helper.CameraHelper
import com.example.cameraxapp.helper.PermissionHelper
import com.example.cameraxapp.ar.ArSupportHelper
import com.example.cameraxapp.ar.GeoArHelper
import com.google.ar.core.Anchor
import com.google.ar.core.ArCoreApk
import androidx.core.net.toUri
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.assets.RenderableSource
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.ux.ArFragment


class MainActivity : AppCompatActivity() {
    private lateinit var viewBinding: ActivityMainBinding

    // helper class for camera
    private lateinit var cameraHelper: CameraHelper

    // background thread for image process
    private lateinit var cameraExecutor: ExecutorService

    private var isArModeActive = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)


        // Warm-up ARCore availability check (ignore result)
        ArCoreApk.getInstance().checkAvailability(this)

        // create background thread
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Initialize CameraHelper
        cameraHelper = CameraHelper(this, viewBinding, cameraExecutor)


        // Request permissions and start camera when granted
        if (PermissionHelper.allPermissionsGranted(this)) {
            cameraHelper.startCamera()
            ArSupportHelper.maybeEnableArButton(this, viewBinding)
        } else {
            requestPermissions()
        }

        // Set up the listeners for take photo and video capture buttons
        viewBinding.imageCaptureButton.setOnClickListener { cameraHelper.takePhoto() }
        viewBinding.videoCaptureButton.setOnClickListener { cameraHelper.captureVideo() }


        // Set up the listeners for AR button
        viewBinding.arButton.setOnClickListener {
            if (!isArModeActive) {
                if (ArSupportHelper.ensureArCoreInstalled(this)) {
                    Toast.makeText(this, "AR mode activated", Toast.LENGTH_SHORT).show()
                    isArModeActive = true
                    viewBinding.arButton.text = getString(R.string.stop_ar)
                    // TODO: add ar feature

                } else {
                    Toast.makeText(this, "ARCore not available or permission missing",
                        Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "AR mode deactivated", Toast.LENGTH_SHORT).show()
                isArModeActive = false
                viewBinding.arButton.text = getString(R.string.start_ar)
                // TODO: remove ar feature

            }

        }



    }


    // check if ARCore is installed and up-to-date on the device
    override fun onResume() {
        super.onResume()

        // Check again if AR is supported & permissions are granted
        if (!PermissionHelper.allPermissionsGranted(this)) {
            return
        }

        if (!ArSupportHelper.isARCoreSupportedAndUpToDate(this)) {
            Toast.makeText(this, "ARCore not available or needs update.", Toast.LENGTH_LONG).show()
        }

        // Retry AR availability check on resume
        ArSupportHelper.maybeEnableArButton(this, viewBinding)


    }

    private fun requestPermissions() {
        activityResultLauncher.launch(PermissionHelper.REQUIRED_PERMISSIONS)
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    private val activityResultLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->

            val missingPermissions = PermissionHelper.getMissingRequiredPermissions(permissions)

            if (missingPermissions.isNotEmpty()) {
                val message = "Missing permission(s): ${missingPermissions.joinToString(", ")}"
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()

                if (PermissionHelper.shouldRedirectToSettings(this)) {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", packageName, null)
                    }
                    startActivity(intent)
                }

                finish()
            } else {
                cameraHelper.startCamera()
            }
        }

}


/*

Arrow by Vanessa Cao
 [CC-BY] (https://creativecommons.org/licenses/by/3.0/) via Poly Pizza (https://poly.pizza/m/bC3FokNqTpi)
 */
