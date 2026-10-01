package com.remmi.core.android.systemInfo

import android.os.Build

/**
 * Android platform implementation of [RemmiSystemInfoCapability].
 */
class AndroidSystemInfoCapability : RemmiSystemInfoCapability {

    override fun getSystemInfo(): RemmiSystemInfo {
        return RemmiSystemInfo(
            osVersion = Build.VERSION.RELEASE ?: "Unknown",
            sdkInt = Build.VERSION.SDK_INT,
            deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}"
        )
    }
}
