package com.remmi.core.android

import com.remmi.core.android.systemInfo.AndroidSystemInfoCapability
import com.remmi.core.android.systemInfo.RemmiSystemInfoCapability
import com.remmi.core.host.RemmiHost
import org.junit.Assert.assertNotNull
import org.junit.Test

class AndroidSystemInfoCapabilityTest {

    @Test
    fun getSystemInfo_returnsValidRemmiSystemInfo() {
        val capability: RemmiSystemInfoCapability = AndroidSystemInfoCapability()
        val info = capability.getSystemInfo()

        assertNotNull("osVersion should not be null", info.osVersion)
        assertNotNull("deviceModel should not be null", info.deviceModel)
    }

    @Test
    fun remmiHost_providesSystemInfoCapability() {
        val host = RemmiHost()
        assertNotNull("Host should expose systemInfoCapability", host.systemInfoCapability)

        val info = host.systemInfoCapability.getSystemInfo()
        assertNotNull("Capability from host should return valid info", info)
    }
}
