package com.example.cameraxapp.ar

import android.app.Activity
import android.util.Log
import android.view.View
import com.google.ar.core.Anchor
import com.google.ar.core.Config
import com.google.ar.core.Earth
import com.google.ar.core.Frame
import com.google.ar.core.GeospatialPose
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import java.util.concurrent.TimeUnit


object GeoArHelper {
    private var session: Session? = null

    // Accuracy thresholds to enter LOCALIZED state.
    // If current accuracy ≤ these values, the app will allow anchor placement.
    private const val LOCALIZING_HORIZONTAL_ACCURACY_THRESHOLD_METERS = 10.0
    private const val LOCALIZING_ORIENTATION_YAW_ACCURACY_THRESHOLD_DEGREES = 15.0

    // Accuracy degradation thresholds to exit LOCALIZED state.
    // If accuracy exceeds these + hysteresis, revert to LOCALIZING.
    private const val LOCALIZED_HORIZONTAL_ACCURACY_HYSTERESIS_METERS = 10.0
    private const val LOCALIZED_ORIENTATION_YAW_ACCURACY_HYSTERESIS_DEGREES = 10.0

    private const val LOCALIZING_TIMEOUT_SECONDS = 180

    private const val MAXIMUM_ANCHORS = 20

    private val anchorsLock = Any()

    private val anchors = mutableListOf<Anchor>()


    /** Timer to keep track of how much time has passed since localizing has started. */
    private var localizingStartTimestamp = 0L

    enum class State {
        /** The Geospatial API has not yet been initialized.  */
        UNINITIALIZED,

        /** The Geospatial API is not supported.  */
        UNSUPPORTED,

        /** The Geospatial API has encountered an unrecoverable error.  */
        EARTH_STATE_ERROR,

        /** The Session has started, but [Earth] isn't [TrackingState.TRACKING] yet.  */
        PRETRACKING,

        /**
         * [Earth] is [TrackingState.TRACKING], but the desired positioning confidence
         * hasn't been reached yet.
         */
        LOCALIZING,

        /** The desired positioning confidence wasn't reached in time.  */
        LOCALIZING_FAILED,

        /**
         * [Earth] is [TrackingState.TRACKING] and the desired positioning confidence has
         * been reached.
         */
        LOCALIZED
    }

    private var state = State.UNINITIALIZED

    // should only need terrain anchor.
    // recommended to use terrain over geospatial
    enum class AnchorType {
        //WGS84 anchors
        GEOSPATIAL,

        //Terrain anchors
        TERRAIN,

        //Rooftop anchors
        ROOFTOP
    }

     fun createSession(activity: Activity): Session? {
        return try {
            val newSession = Session(activity)
            if (!newSession.isGeospatialModeSupported(Config.GeospatialMode.ENABLED)) {
                Log.e("GeoArHelper", "Geospatial mode is not supported.")
                state = State.UNSUPPORTED
                return null
            }
            val config = Config(newSession).apply {
                geospatialMode = Config.GeospatialMode.ENABLED
                // might want to enable for future
                //streetscapeGeometryMode = Config.StreetscapeGeometryMode.ENABLED
            }
            newSession.configure(config)
            state = State.PRETRACKING
            localizingStartTimestamp = System.currentTimeMillis()
            session = newSession
            session
        } catch (e: Exception) {
            Log.e("GeoArHelper", "Session creation error: ${e.localizedMessage}")
            null
        }
    }

    /** Change behavior depending on the current [State] of the application.  */
    fun updateGeospatialState(earth: Earth) {
        if (earth.earthState != Earth.EarthState.ENABLED) {
            state = State.EARTH_STATE_ERROR
            return
        }
        if (earth.trackingState != TrackingState.TRACKING) {
            Log.i("GeoArHelper", "1")

            state = State.PRETRACKING
            return
        }
        if (state == State.PRETRACKING) {

            updatePretrackingState(earth)
        } else if (state == State.LOCALIZING) {
            updateLocalizingState(earth)
        } else if (state == State.LOCALIZED) {
            updateLocalizedState(earth)
        }
    }

    /**
     * Handles the updating for [State.PRETRACKING]. In this state, wait for [Earth] to
     * have [TrackingState.TRACKING]. If it hasn't been enabled by now, then we've encountered
     * an unrecoverable [State.EARTH_STATE_ERROR].
     */
    private fun updatePretrackingState(earth: Earth) {
        if (earth.trackingState == TrackingState.TRACKING) {
            state = State.LOCALIZING
            return
        }
        // can add UI for user later using geospatial_pose_view
        //runOnUiThread(Runnable { geospatialPoseTextView.setText("GEOSPATIAL POSE: Not tracking") })
        Log.i("GeoArHelper", "GEOSPATIAL POSE: Not tracking")

    }

    /**
     * Handles the updating for [State.LOCALIZING]. In this state, wait for the horizontal and
     * orientation threshold to improve until it reaches your threshold.
     *
     *
     * If it takes too long for the threshold to be reached, this could mean that GPS data isn't
     * accurate enough, or that the user is in an area that can't be localized with StreetView.
     */
    private fun updateLocalizingState(earth: Earth) {
        val geospatialPose = earth.cameraGeospatialPose
        if (geospatialPose.horizontalAccuracy <= LOCALIZING_HORIZONTAL_ACCURACY_THRESHOLD_METERS
            && geospatialPose.orientationYawAccuracy
                    <= LOCALIZING_ORIENTATION_YAW_ACCURACY_THRESHOLD_DEGREES) {
            state = State.LOCALIZED
            synchronized(anchorsLock) {
                val anchorNum = anchors.size
                if (anchorNum == 0) {
                    //createAnchorFromSharedPreferences(earth)
                    Log.i("anchors","create anchor")
                }
                if (anchorNum < MAXIMUM_ANCHORS) {
                    Log.i("anchors","create anchor2")

//                    runOnUiThread(
//                        Runnable {
//                            setAnchorButton.setVisibility(View.VISIBLE)
//                            tapScreenTextView.setVisibility(View.VISIBLE)
//                            if (anchorNum > 0) {
//                                clearAnchorsButton.setVisibility(View.VISIBLE)
//                            }
//                        })
                }
            }
            return
        }

        if (TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - localizingStartTimestamp)
            > LOCALIZING_TIMEOUT_SECONDS) {
            state = State.LOCALIZING_FAILED
            return
        }

        Log.i("geospatialPose", {geospatialPose}.toString())
    }

    /**
     * Handles the updating for [State.LOCALIZED]. In this state, check the accuracy for
     * degradation and return to [State.LOCALIZING] if the position accuracies have dropped too
     * low.
     */
    private fun updateLocalizedState(earth: Earth) {
        val geospatialPose = earth.cameraGeospatialPose
        // Check if either accuracy has degraded to the point we should enter back into the LOCALIZING
        // state.
        if (geospatialPose.horizontalAccuracy > LOCALIZING_HORIZONTAL_ACCURACY_THRESHOLD_METERS
                    + LOCALIZED_HORIZONTAL_ACCURACY_HYSTERESIS_METERS
            || geospatialPose.orientationYawAccuracy > LOCALIZING_ORIENTATION_YAW_ACCURACY_THRESHOLD_DEGREES
            + LOCALIZED_ORIENTATION_YAW_ACCURACY_HYSTERESIS_DEGREES
        ) {
            // Accuracies have degenerated, return to the localizing state.
            state = State.LOCALIZING
            localizingStartTimestamp = System.currentTimeMillis()
//            runOnUiThread(
//                Runnable {
//                    setAnchorButton.setVisibility(View.INVISIBLE)
//                    tapScreenTextView.setVisibility(View.INVISIBLE)
//                    clearAnchorsButton.setVisibility(View.INVISIBLE)
//                })
            return
        }

        Log.i("geospatialPose", {geospatialPose}.toString())
    }



    fun closeSession() {
        if (session != null) {
            session?.close()
            session = null
        }
    }

    fun getSession(): Session? = session

    fun createAnchorAt(lat: Double, lng: Double, alt: Double): Anchor? {
        val earth = session?.earth ?: return null
        if (earth.trackingState != TrackingState.TRACKING) return null
        return earth.createAnchor(lat, lng, alt, 0f, 0f, 0f, 1f)
    }


}


//    fun getGeospatialPose(frame: Frame): GeospatialPose? {
//        val earth = session?.earth
//        return if (earth?.trackingState == TrackingState.TRACKING) {
//            earth.cameraGeospatialPose
//        } else null
//    }