package com.infinikey_ime

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.infinikey_ime.util.FontFallbackManager

class InfinikeyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FontFallbackManager.init(this)

        // Automatically apply fallback Typeface to all TextViews / EditTexts created across all Activities
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                activity.window.decorView.post {
                    FontFallbackManager.applyToView(activity.window.decorView)
                }
            }

            override fun onActivityStarted(activity: Activity) {}

            override fun onActivityResumed(activity: Activity) {
                activity.window.decorView.post {
                    FontFallbackManager.applyToView(activity.window.decorView)
                }
            }

            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }
}
