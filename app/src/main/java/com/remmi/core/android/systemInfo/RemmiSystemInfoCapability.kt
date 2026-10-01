package com.remmi.core.android.systemInfo

/**
 * Public Core capability contract for querying basic system information.
 */
interface RemmiSystemInfoCapability {

    /**
     * Returns basic system information about the Android device environment.
     */
    fun getSystemInfo(): RemmiSystemInfo
}
