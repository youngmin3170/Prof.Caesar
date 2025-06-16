package com.example.cameraxapp.ar

import android.app.Activity
import android.util.Log
import com.google.ar.core.*
import com.google.ar.core.exceptions.*

object GeoArHelper {

    private var session: Session? = null


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
        } catch (e: UnavailableException) {
            Log.e("GeoArHelper", "Failed to create AR session: ${e.localizedMessage}")
            null
        } catch (e: UnsupportedConfigurationException) {
            Log.e("GeoArHelper", "Unsupported config: ${e.localizedMessage}")
            null
        }
    }


}