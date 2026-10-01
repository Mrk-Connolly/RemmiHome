package com.remmi.core

import android.app.Application
import com.remmi.core.android.launcher.AndroidLauncherCapability
import com.remmi.core.controller.RemmiController
import com.remmi.core.host.RemmiHost

/**
 * Fundamental Remmi core application entry point.
 */
class RemmiApplication : Application() {

    lateinit var host: RemmiHost
        private set

    lateinit var controller: RemmiController
        private set

    override fun onCreate() {
        super.onCreate()
        val launcherCapability = AndroidLauncherCapability(applicationContext)
        host = RemmiHost(launcherCapability = launcherCapability)
        controller = RemmiController(host)
    }
}
