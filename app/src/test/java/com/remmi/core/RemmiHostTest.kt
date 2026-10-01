package com.remmi.core

import com.remmi.core.controller.RemmiController
import com.remmi.core.host.RemmiHost
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Test

class RemmiHostTest {

    @Test
    fun remmiHost_initializesWithEventBusAndScope() {
        val host = RemmiHost()
        assertNotNull("Host should own EventBus", host.eventBus)
        assertNotNull("Host should own CoroutineScope", host.scope)
    }

    @Test
    fun remmiController_linksToHost() {
        val host = RemmiHost()
        val controller = RemmiController(host)
        assertSame("Controller should link to Host", host, controller.host)
    }
}
