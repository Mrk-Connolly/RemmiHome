package com.remmi.ui.home

import com.remmi.plugins.planning.calendar.models.CalendarEvent
import com.remmi.plugins.planning.calendar.repository.CalendarEventRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class HomeCalendarWidgetTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun homeCalendarWidget_fetchesUpcomingEventsSortedByDateAndTime() = runBlocking {
        val repo = CalendarEventRepository(storageDir = tempFolder.root)

        // Event today later
        repo.addEvent(CalendarEvent(title = "Afternoon Sync", startDateIso = "2026-10-05", startTimeIso = "14:00"))
        // Event today earlier
        repo.addEvent(CalendarEvent(title = "Morning Standup", startDateIso = "2026-10-05", startTimeIso = "09:00"))
        // Event tomorrow
        repo.addEvent(CalendarEvent(title = "Project Review", startDateIso = "2026-10-06", startTimeIso = "10:00"))
        // Event past date
        repo.addEvent(CalendarEvent(title = "Old Event", startDateIso = "2026-09-01", startTimeIso = "10:00"))

        val upcoming = repo.getEventsForRange("2026-10-05", "2026-10-19")
            .sortedWith(compareBy({ it.startDateIso }, { it.startTimeIso ?: "" }))

        assertEquals(3, upcoming.size)
        assertEquals("Morning Standup", upcoming[0].title)
        assertEquals("Afternoon Sync", upcoming[1].title)
        assertEquals("Project Review", upcoming[2].title)
    }

    @Test
    fun homeCalendarWidget_gracefullyReturnsEmptyWhenNoEvents() = runBlocking {
        val repo = CalendarEventRepository(storageDir = tempFolder.root)

        val upcoming = repo.getEventsForRange("2026-10-05", "2026-10-19")

        assertTrue("Upcoming events should be empty when repo has no events", upcoming.isEmpty())
    }

    @Test
    fun homeCalendarWidget_handlesAllDayAndTimedEventsCorrectly() = runBlocking {
        val repo = CalendarEventRepository(storageDir = tempFolder.root)

        repo.addEvent(CalendarEvent(title = "Company Holiday", startDateIso = "2026-10-05", isAllDay = true))
        repo.addEvent(CalendarEvent(title = "Doctor Appointment", startDateIso = "2026-10-05", startTimeIso = "11:00", endTimeIso = "12:00"))

        val upcoming = repo.getEventsForRange("2026-10-05", "2026-10-05")

        assertEquals(2, upcoming.size)
        assertTrue(upcoming.any { it.isAllDay && it.title == "Company Holiday" })
        assertTrue(upcoming.any { !it.isAllDay && it.startTimeIso == "11:00" && it.endTimeIso == "12:00" })
    }
}
