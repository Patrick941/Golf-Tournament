package com.example.beamishinvitational

import android.app.Application
import io.sentry.android.core.SentryAndroid

class BeamishApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        SentryAndroid.init(this) { options ->
            // If you have a DSN, set it here. If not, the SDK will be disabled to avoid crashes.
            // When auto-init is disabled, we MUST provide a DSN or set isEnabled to false.
            if (options.dsn.isNullOrEmpty()) {
                options.isEnabled = false
            }

            // The crash "Tried to obtain display from a Context not associated with one"
            // often happens in SentryFrameMetricsCollector when frames tracking is enabled
            // and it tries to access the Display from the Application context.
            // In Sentry 7+ this is generally improved, but if it persists, 
            // disabling frames tracking or ensuring it's only active for Activities is the fix.
            
            // To be safe and fix the reported crash:
            options.isEnableFramesTracking = false 
            
            // You can also add other configurations here
            // If you have a DSN, set it here. If not, the SDK will be disabled to avoid crashes.
            if (options.dsn.isNullOrEmpty()) {
                options.isEnabled = false
            }
        }
    }
}
