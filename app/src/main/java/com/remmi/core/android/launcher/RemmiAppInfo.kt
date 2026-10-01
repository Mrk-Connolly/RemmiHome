package com.remmi.core.android.launcher

/**
 * Represents an installed Android application launchable from Remmi Home.
 */
data class RemmiAppInfo(
    val packageName: String,
    val activityName: String,
    val label: String
)
