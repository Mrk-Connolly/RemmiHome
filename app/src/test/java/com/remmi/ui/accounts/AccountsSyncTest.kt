package com.remmi.ui.accounts

import com.remmi.core.android.google.auth.DefaultGoogleAuthCapability
import com.remmi.core.android.google.auth.RemmiGoogleAccount
import com.remmi.core.android.google.calendar.DefaultGoogleCalendarCapability
import com.remmi.core.android.google.calendar.RemmiCalendarEvent
import com.remmi.core.host.RemmiHost
import com.remmi.plugins.planning.calendar.repository.CalendarEventRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class AccountsSyncTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun host_providesDefaultGoogleCapabilities() {
        val host = RemmiHost()

        assertNotNull(host.googleAuthCapability)
        assertNotNull(host.calendarCapability)
    }

    @Test
    fun accountsSync_googleAuthSignInAndOut() = runBlocking {
        val authCapability = DefaultGoogleAuthCapability()

        val result = authCapability.signIn()
        assertTrue(result)
        assertTrue(authCapability.isSignedIn)
        assertEquals("user@gmail.com", authCapability.currentAccount.value?.email)

        val signOutResult = authCapability.signOut()
        assertTrue(signOutResult)
        assertEquals(null, authCapability.currentAccount.value)
    }

    @Test
    fun accountsSync_googleCalendarEventsSyncIntoRepository() = runBlocking {
        val account = RemmiGoogleAccount("123", "test@gmail.com", "Test User")
        val authCapability = DefaultGoogleAuthCapability(initialAccount = account)

        val sampleGoogleEvents = listOf(
            RemmiCalendarEvent("ev_101", "Google Meeting", "09:00", "10:00", false),
            RemmiCalendarEvent("ev_102", "Team Lunch", "12:00", "13:00", false)
        )
        val calendarCapability = DefaultGoogleCalendarCapability(
            authCapability = authCapability,
            initialCachedEvents = sampleGoogleEvents
        )

        val repo = CalendarEventRepository(storageDir = tempFolder.root)

        val syncedEvents = repo.syncWithGoogleCalendar(
            googleAccountId = account.accountId,
            calendarCapability = calendarCapability
        )

        assertEquals(2, syncedEvents.size)
        assertTrue(syncedEvents.any { it.title == "Google Meeting" && it.googleAccountId == "123" })
        assertTrue(syncedEvents.any { it.title == "Team Lunch" && it.googleAccountId == "123" })
        assertEquals(2, repo.events.value.size)
    }
}
