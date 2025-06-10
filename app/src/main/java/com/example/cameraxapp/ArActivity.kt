package com.example.cameraxapp

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.ar.core.Anchor
import com.google.ar.core.ArCoreApk
import com.google.ar.core.exceptions.UnavailableException
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode

class ArActivity : AppCompatActivity() {

    private val REQUIRED_PERMISSIONS = arrayOf(android.Manifest.permission.CAMERA)
    private val REQUEST_CODE_PERMISSIONS = 10

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        checkSelfPermission(it) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }


    private lateinit var arFragment: ArFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ar)

        if (!allPermissionsGranted()) {
            requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
            return
        }

        // Verify that ARCore is installed and using the current version.
        fun isARCoreSupportedAndUpToDate(): Boolean {
            return when (ArCoreApk.getInstance().checkAvailability(this)) {
                ArCoreApk.Availability.SUPPORTED_INSTALLED -> true
                ArCoreApk.Availability.SUPPORTED_APK_TOO_OLD, ArCoreApk.Availability.SUPPORTED_NOT_INSTALLED -> {
                    try {
                        // Request ARCore installation or update if needed.
                        when (ArCoreApk.getInstance().requestInstall(this, true)) {
                            ArCoreApk.InstallStatus.INSTALL_REQUESTED -> {
                                Toast.makeText(this, "ARCore installation requested", Toast.LENGTH_LONG).show()
                                false
                            }
                            ArCoreApk.InstallStatus.INSTALLED -> true
                            ArCoreApk.InstallStatus.INSTALLED -> TODO()
                        }
                    } catch (e: UnavailableException) {
                        Toast.makeText(this, "ARCore installation failed: $e", Toast.LENGTH_LONG).show()
                        false
                    }
                }

                ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE ->
                    // This device is not supported for AR.
                    false

                ArCoreApk.Availability.UNKNOWN_CHECKING -> {
                    // ARCore is checking the availability with a remote query.
                    // This function should be called again after waiting 200 ms to determine the query result.
                }
                ArCoreApk.Availability.UNKNOWN_ERROR, ArCoreApk.Availability.UNKNOWN_TIMED_OUT -> {
                    // There was an error checking for AR availability. This may be due to the device being offline.
                    // Handle the error appropriately.
                }
            }
        }

        arFragment = supportFragmentManager.findFragmentById(R.id.ux_fragment) as ArFragment

        // Optional: handle taps to place arrows or objects
        arFragment.setOnTapArPlaneListener { hitResult, _, _ ->
            val anchor = hitResult.createAnchor()
            placeObject(anchor)
        }
    }

    private fun placeObject(anchor: Anchor) {
        val anchorNode = AnchorNode(anchor)
        anchorNode.setParent(arFragment.arSceneView.scene)

        val transformableNode = TransformableNode(arFragment.transformationSystem)
        transformableNode.setParent(anchorNode)

        // You can load 3D models here. For now, use a simple arrow model if available.
        ModelRenderable.builder()
            .setSource(this, Uri.parse("arrow.sfb"))  // or .glb/.gltf
            .build()
            .thenAccept { renderable ->
                transformableNode.renderable = renderable
                transformableNode.select()
            }
            .exceptionally {
                Toast.makeText(this, "Error loading model", Toast.LENGTH_SHORT).show()
                null
            }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                recreate() // Restart activity to re-init AR view
            } else {
                Toast.makeText(this, "Camera permission is required for AR", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

}
