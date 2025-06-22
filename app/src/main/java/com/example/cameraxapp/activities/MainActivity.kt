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

class MainActivity : AppCompatActivity() {
    private lateinit var viewBinding: ActivityMainBinding

    // helper class for camera
    private lateinit var cameraHelper: CameraHelper

    // background thread for image process
    private lateinit var cameraExecutor: ExecutorService


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
            if (ArSupportHelper.ensureArCoreInstalled(this)) {
                Toast.makeText(this, "AR mode activated", Toast.LENGTH_SHORT).show()

                // TODO: add ar feature

            } else {
                Toast.makeText(this, "ARCore not available or permission missing",
                    Toast.LENGTH_SHORT).show()
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
package com.example.cameraxapp.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.cameraxapp.R
import com.example.cameraxapp.ar.ArSupportHelper
import com.example.cameraxapp.ar.GeoArHelper
import com.example.cameraxapp.databinding.ActivityMainBinding
import com.example.cameraxapp.helper.CameraHelper
import com.example.cameraxapp.helper.PermissionHelper
import com.google.ar.core.Anchor
import com.google.ar.core.ArCoreApk
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.ux.TransformableNode
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import androidx.core.net.toUri
import com.google.ar.sceneform.assets.RenderableSource

class MainActivity : AppCompatActivity() {
    private lateinit var viewBinding: ActivityMainBinding

    // helper class for camera
    private lateinit var cameraHelper: CameraHelper

    // background thread for image process
    private lateinit var cameraExecutor: ExecutorService

    private lateinit var arSceneView: ArSceneView
    private var arrowRenderable: ModelRenderable? = null
    private var arMode = false

    // change authoringMode to false for users later
    private val authoringMode = true


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        // create background thread
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Initialize CameraHelper
        cameraHelper = CameraHelper(this, viewBinding, cameraExecutor)

        // Warm-up ARCore availability check (ignore result)
        ArCoreApk.getInstance().checkAvailability(this)

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
        viewBinding.arButton.setOnClickListener { toggleArMode() }

        val uri = "arrow.glb".toUri()
        val renderableSource = RenderableSource.builder()
            .setSource(this, uri, RenderableSource.SourceType.GLB)
            .setScale(1.0f)                 // optional
            .setRecenterMode(RenderableSource.RecenterMode.ROOT)
            .build()

        ModelRenderable.builder()
            .setSource(this, "arrow.glb".toUri())
            .setIsFilamentGltf(true)
            .build()
            .thenAccept { arrowRenderable = it }
            .exceptionally {
                Toast.makeText(this, "Failed to load arrow model", Toast.LENGTH_LONG).show()
                null
            }

        arSceneView = viewBinding.arSceneView
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

        if (arMode) {
            GeoArHelper.getSession()?.resume()
            arSceneView.resume()
        }

    }

    override fun onPause() {
        super.onPause()
        if (arMode) {
            arSceneView.pause()
            GeoArHelper.getSession()?.pause()
        }
    }



    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    private fun requestPermissions() {
        activityResultLauncher.launch(PermissionHelper.REQUIRED_PERMISSIONS)
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

    private fun toggleArMode() {
        if (!ArSupportHelper.ensureArCoreInstalled(this)) {
            Toast.makeText(this, "Install/Update ARCore first.", Toast.LENGTH_SHORT).show()
            return
        }

        arMode = !arMode
        if (arMode) enableArLayer() else disableArLayer()
    }

    private fun enableArLayer() {
        Toast.makeText(this, "AR mode ON", Toast.LENGTH_SHORT).show()

        // 1. Create / configure geospatial session
        val session = GeoArHelper.createSession(this) ?: run {
            Toast.makeText(this, "Failed to create AR session", Toast.LENGTH_LONG).show()
            return
        }
        arSceneView.setupSession(session)

        // 2. Show the ArSceneView overlay
        arSceneView.visibility = View.VISIBLE

        // 3. Tap listener → hit-test → terrain anchor
        arSceneView.setOnTouchListener { _, motionEvent ->
            val frame = arSceneView.arFrame ?: return@setOnTouchListener false
            if (motionEvent.action == MotionEvent.ACTION_UP && authoringMode) {
                GeoArHelper.performHitTestAndPlaceTerrainAnchor(
                    frame,
                    motionEvent,
                    onAnchorReady = { anchor -> renderArrowModel(anchor) },
                    onError = { msg -> Toast.makeText(this, msg, Toast.LENGTH_SHORT).show() }
                )
                true
            } else false
        }

        // 4. On every frame, once tracking, re-place any saved anchors
        arSceneView.scene.addOnUpdateListener { _ ->
            val frame = arSceneView.arFrame ?: return@addOnUpdateListener
            val gePose = GeoArHelper.getGeospatialPose(frame) ?: return@addOnUpdateListener

            // Only run once (listener removed after first success)
            arSceneView.scene.removeOnUpdateListener(this)

            GeoArHelper.getSavedAnchorLocations().forEach { (lat, lng, alt) ->
                val anchor = GeoArHelper.createAnchorAt(lat, lng, alt)
                if (anchor != null) renderArrowModel(anchor)
            }
        }
    }

    /** Hides the AR overlay & pauses session. */
    private fun disableArLayer() {
        Toast.makeText(this, "AR mode OFF", Toast.LENGTH_SHORT).show()
        arSceneView.visibility = View.GONE
        arSceneView.pause()
        GeoArHelper.getSession()?.pause()
    }

    private fun renderArrowModel(anchor: Anchor) {
        val renderable = arrowRenderable ?: return
        val anchorNode = AnchorNode(anchor).also {
            it.setParent(arSceneView.scene)
        }
        TransformableNode(arSceneView.transformationSystem).apply {
            this.renderable = renderable
            setParent(anchorNode)
            select()
        }
    }
}

 */