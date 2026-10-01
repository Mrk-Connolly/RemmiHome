package com.remmi.core.android.systemInfo

/**
 * Remmi-owned data model representing system environment details.
 */
data class RemmiSystemInfo(
    val osVersion: String,
    val sdkInt: Int,
    val deviceModel: String
)
