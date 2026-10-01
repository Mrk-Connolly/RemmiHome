package com.remmi.core.eventBus.commands

import com.remmi.core.android.launcher.RemmiAppInfo

/**
 * Core command representing an explicit request to launch an Android application.
 *
 * Implements [RemmiCommand] ("something should happen").
 * Contains only Remmi model primitives ([RemmiAppInfo]), free of Android platform references
 * (Context, Activity, PackageManager, Intent).
 */
data class LaunchAppCommand(
    val appInfo: RemmiAppInfo
) : RemmiCommand
