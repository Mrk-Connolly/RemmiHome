package com.remmi.core

import android.app.Application
import com.remmi.core.android.launcher.AndroidLauncherCapability
import com.remmi.core.controller.RemmiController
import com.remmi.core.host.RemmiHost
import com.remmi.plugins.food.FoodPlugin
import com.remmi.plugins.health.HealthPlugin
import com.remmi.plugins.knowledge.KnowledgePlugin
import com.remmi.plugins.planning.PlanningPlugin

import com.remmi.core.android.google.auth.DefaultGoogleAuthCapability
import com.remmi.core.ai.DefaultAiCapability

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
        val authCapability = DefaultGoogleAuthCapability(context = applicationContext)
        host = RemmiHost(
            launcherCapability = launcherCapability,
            googleAuthCapability = authCapability,
            aiCapability = DefaultAiCapability()
        )
        controller = RemmiController(host)

        // Register default workspace feature plugins with local storage directory
        host.pluginRegistry.register(PlanningPlugin(storageDir = filesDir))
        host.pluginRegistry.register(FoodPlugin())
        host.pluginRegistry.register(HealthPlugin(storageDir = filesDir))
        host.pluginRegistry.register(KnowledgePlugin(storageDir = filesDir))

        // Asynchronous non-blocking background capability & plugin warming
        host.warmCapabilitiesAsync()
    }
}
