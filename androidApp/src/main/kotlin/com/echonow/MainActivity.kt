package com.echonow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.echonow.app.BuildConfig
import com.echonow.platform.AndroidPlatformContext
import com.echonow.platform.hideSystemBarsAfterDelay
import com.echonow.presentation.navigation.EchoNowApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidPlatformContext.initialize(
            this,
            BuildConfig.THEMOVIEDB_API_KEY,
            BuildConfig.THEMOVIEDB_API_READ_ACCESS_TOKEN,
            BuildConfig.SUBDL_API_KEY,
            BuildConfig.APP_NAME,
            BuildConfig.APP_VERSION,
            BuildConfig.APP_WEB,
        )
        setContent { EchoNowApp() }
        hideSystemBarsAfterDelay()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBarsAfterDelay()
    }
}
