package com.example.cameraxapp.ar

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.MotionEvent
import com.google.ar.core.*
import java.util.concurrent.Executor
import java.util.function.BiConsumer


object GeoArHelper {
    private var session: Session? = null

    private val savedAnchors = mutableListOf<Triple<Double, Double, Double>>()  // lat, lng, alt

    fun createSession(activity: Activity): Session? {
        return try {
            val newSession = Session(activity)
            if (!newSession.isGeospatialModeSupported(Config.GeospatialMode.ENABLED)) {
                Log.e("GeoArHelper", "Geospatial mode is not supported.")
                return null
            }
            val config = Config(newSession).apply {
                geospatialMode = Config.GeospatialMode.ENABLED
            }
            newSession.configure(config)
            session = newSession
            session
        } catch (e: Exception) {
            Log.e("GeoArHelper", "Session creation error: ${e.localizedMessage}")
            null
        }
    }

    fun getSession(): Session? = session

    fun getGeospatialPose(frame: Frame): GeospatialPose? {
        val earth = session?.earth
        return if (earth?.trackingState == TrackingState.TRACKING) {
            earth.cameraGeospatialPose
        } else null
    }

    fun createAnchorAt(lat: Double, lng: Double, alt: Double): Anchor? {
        val earth = session?.earth ?: return null
        if (earth.trackingState != TrackingState.TRACKING) return null
        return earth.createAnchor(lat, lng, alt, 0f, 0f, 0f, 1f)
    }

    fun saveAnchorLocation(lat: Double, lng: Double, alt: Double) {
        savedAnchors.add(Triple(lat, lng, alt))
    }

    fun getSavedAnchorLocations(): List<Triple<Double, Double, Double>> = savedAnchors
}

/*
fun performHitTestAndPlaceTerrainAnchor(
        frame: Frame,
        motionEvent: MotionEvent,
        onAnchorReady: (Anchor) -> Unit,
        onError: (String) -> Unit
    ) {
        val session = session ?: return onError("Session is null.")
        val earth = session.earth ?: return onError("Earth is null.")
        if (earth.trackingState != TrackingState.TRACKING) {
            return onError("Earth tracking not available.")
        }

        val hitResults = frame.hitTest(motionEvent)
        for (hit in hitResults) {
            val pose = hit.hitPose
            val geoPose = earth.getGeospatialPose(pose)
            val quaternion = geoPose.eastUpSouthQuaternion

            earth.resolveAnchorOnTerrainAsync(
                geoPose.latitude,
                geoPose.longitude,
                0.0,
                quaternion[0], quaternion[1], quaternion[2], quaternion[3],
                BiConsumer { anchor, state ->
                    if (state == Anchor.TerrainAnchorState.SUCCESS && anchor != null) {
                        saveAnchorLocation(geoPose.latitude, geoPose.longitude, geoPose.altitude)
                        onAnchorReady(anchor)
                    } else {
                        onError("Terrain anchor failed with state: $state")
                    }
                }
            )
            return
        }

        onError("No suitable hit result found.")
    }


 */