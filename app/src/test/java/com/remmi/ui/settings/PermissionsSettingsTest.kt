package com.remmi.ui.settings

import com.remmi.core.android.alarm.DefaultAlarmCapability
import com.remmi.core.android.audio.AndroidAudioCapability
import com.remmi.core.android.notification.DefaultNotificationCapability
import com.remmi.core.host.RemmiHost
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionsSettingsTest {

    @Test
    fun `remmi host holds android capability references for settings permissions`() {
        val host = RemmiHost()

        assertNotNull(host.audioCapability)
        assertNotNull(host.alarmCapability)
        assertNotNull(host.notificationCapability)

        assertTrue(host.audioCapability is AndroidAudioCapability)
        assertTrue(host.alarmCapability is DefaultAlarmCapability)
        assertTrue(host.notificationCapability is DefaultNotificationCapability)
    }
}
