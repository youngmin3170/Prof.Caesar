package com.example.cameraxapp.activities

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.cameraxapp.ar.ArSupportHelper
import com.example.cameraxapp.databinding.ActivityArBinding
import com.example.cameraxapp.helper.PermissionHelper
import com.google.ar.core.TrackingState
import com.google.ar.core.Anchor.TerrainAnchorState
import com.google.ar.core.Config
import com.google.ar.core.Session
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.node.ModelNode

class ArActivity : AppCompatActivity() {

    private lateinit var binding: ActivityArBinding
    private lateinit var modelNode: ModelNode

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityArBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Bind lifecycle to ARSceneView
        binding.sceneView.lifecycle = lifecycle

//        val session = Session(this)
//
//        session.configure(session.config.apply { geospatialMode = Config.GeospatialMode.ENABLED })
        binding.sceneView.configureSession { session, config ->
            config.geospatialMode = Config.GeospatialMode.ENABLED
        }


//        // Load the model node (do not anchor yet)
//        modelNode = ModelNode(
//            modelInstance = binding.sceneView.modelLoader.createModelInstance(
//                assetFileLocation = "models/arrow.glb"
//            ),
//            scaleToUnits = 1.0f,
//            centerOrigin = io.github.sceneview.math.Position(0.0f)
//        )

        Toast.makeText(this, "Model loaded", Toast.LENGTH_SHORT).show()

        binding.placeAnchorButton.setOnClickListener {
            val earth = binding.sceneView.session?.earth

//            val earth = session.earth
            Log.d("GeoAR", "Earth state: ${earth?.trackingState}")

            if (earth?.trackingState == TrackingState.TRACKING) {
                val pose = earth.cameraGeospatialPose
                val rotation = pose.eastUpSouthQuaternion

                Log.d("GeoAR", "lat: ${pose.latitude}")
                Log.d("GeoAR", "lon: ${pose.longitude}")

                val anchorPose = com.google.ar.core.Pose(
                    floatArrayOf(0f, 0f, -1f),  // 1 meter in front of the origin
                    pose.eastUpSouthQuaternion  // orientation from camera
                )

                val anchor = binding.sceneView.session?.createAnchor(anchorPose)
                if (anchor != null) {
                    val modelInstance = binding.sceneView.modelLoader.createModelInstance("models/arrow.glb")

                    val modelNode = ModelNode(
                        modelInstance = modelInstance,
                        scaleToUnits = 1.0f,
                        centerOrigin = io.github.sceneview.math.Position(0f)
                    )

                    val anchorNode = AnchorNode(
                        engine = binding.sceneView.engine,
                        anchor = anchor
                    ).apply {
                        addChildNode(modelNode)
                    }

                    binding.sceneView.addChildNode(anchorNode)
                    Toast.makeText(this, "Model placed in front of camera", Toast.LENGTH_SHORT).show()
                }
//
//                earth.resolveAnchorOnTerrainAsync(
//                    pose.latitude,
//                    //40.7580,
//                    //-73.9855,
//                            pose.longitude,
//                    1.6,  // Height above ground
//                    rotation[0], rotation[1], rotation[2], rotation[3]
//                ) { anchor, state ->
//                    Log.d("GeoAR", "Anchor state: $state")
//
//                    if (state == TerrainAnchorState.SUCCESS) {
//                        val modelInstance = binding.sceneView.modelLoader.createModelInstance("models/arrow.glb")
//
//                        val modelNode = ModelNode(
//                            modelInstance = modelInstance,
//                            scaleToUnits = 1.0f,
//                            centerOrigin = io.github.sceneview.math.Position(0.0f)
//                        )
//                        val anchorNode = AnchorNode(
//                            engine = binding.sceneView.engine,
//                            anchor = anchor
//                        ).apply {
//                            addChildNode(modelNode)
//                        }
//                        binding.sceneView.addChildNode(anchorNode)
//                        Toast.makeText(this, "Anchor placed", Toast.LENGTH_SHORT).show()
//                    } else {
//                        Toast.makeText(this, "Failed to resolve anchor: $state", Toast.LENGTH_SHORT).show()
//                    }
//                }
            } else {
                Toast.makeText(this, "Earth not tracking yet", Toast.LENGTH_SHORT).show()
            }
        }
    }

}
