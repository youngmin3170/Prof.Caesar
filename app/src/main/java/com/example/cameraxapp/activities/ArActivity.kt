package com.example.cameraxapp.activities

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.cameraxapp.databinding.ActivityArBinding
import com.google.ar.core.TrackingState
import com.google.ar.core.Anchor.TerrainAnchorState
import io.github.sceneview.ar.ARSceneView
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

        // Load the model node (do not anchor yet)
        modelNode = ModelNode(
            modelInstance = binding.sceneView.modelLoader.createModelInstance(
                assetFileLocation = "models/arrow.glb"
            ),
            scaleToUnits = 1.0f,
            centerOrigin = io.github.sceneview.math.Position(0.0f)
        )

        Toast.makeText(this, "Model loaded", Toast.LENGTH_SHORT).show()

        binding.placeAnchorButton.setOnClickListener {
            val earth = binding.sceneView.session?.earth
            if (earth?.trackingState == TrackingState.TRACKING) {
                val pose = earth.cameraGeospatialPose
                val rotation = pose.eastUpSouthQuaternion

                earth.resolveAnchorOnTerrainAsync(
                    pose.latitude,
                    pose.longitude,
                    1.6,  // Height above ground
                    rotation[0], rotation[1], rotation[2], rotation[3]
                ) { _, state ->
                    if (state == TerrainAnchorState.SUCCESS) {
                        binding.sceneView.addChildNode(modelNode)
                        Toast.makeText(this, "Anchor placed", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Failed to resolve anchor: $state", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "Earth not tracking yet", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
