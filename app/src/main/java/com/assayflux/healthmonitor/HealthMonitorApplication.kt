package com.assayflux.healthmonitor

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import android.view.WindowManager

/**
 * Initializes native database support before any Activity or database access,
 * then applies privacy protections to every app screen.
 */
class HealthMonitorApplication : Application() {

    @Volatile
    var sqlCipherInitializationError: String? = null
        private set

    override fun onCreate() {
        super.onCreate()

        // SQLCipher's AAR contains libsqlcipher.so, but the app must load it
        // explicitly before SQLiteOpenHelper or any SQLCipher database access.
        try {
            System.loadLibrary("sqlcipher")
        } catch (error: LinkageError) {
            sqlCipherInitializationError = error.message ?: error.javaClass.simpleName
            Log.e(TAG, "SQLCipher native library initialization failed", error)
        }

        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, state: Bundle?) {
                activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }

            override fun onActivityStarted(activity: Activity) = Unit
            override fun onActivityResumed(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivityStopped(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })
    }

    private companion object {
        const val TAG = "HealthMonitorApp"
    }
}
