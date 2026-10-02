package com.remmi.core.android.launcher

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Represents an installed Android application launchable from Remmi Home.
 */
data class RemmiAppInfo(
    val packageName: String,
    val activityName: String,
    val label: String,
    val icon: ImageBitmap? = null
)
