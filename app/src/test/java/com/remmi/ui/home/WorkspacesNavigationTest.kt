package com.remmi.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class WorkspacesNavigationTest {

    @Test
    fun `workspace domains include planning and knowledge`() {
        val domains = WorkspaceDomain.entries
        assertEquals(6, domains.size)
        assertEquals(WorkspaceDomain.PLANNING, domains[0])
        assertEquals(WorkspaceDomain.KNOWLEDGE, domains[1])
        assertEquals(WorkspaceDomain.HEALTH, domains[2])
        assertEquals(WorkspaceDomain.TRAVEL, domains[3])
        assertEquals(WorkspaceDomain.HOUSEHOLD, domains[4])
        assertEquals(WorkspaceDomain.PEOPLE, domains[5])
    }

    @Test
    fun `workspace domain symbols and titles are non empty`() {
        WorkspaceDomain.entries.forEach { domain ->
            assertNotNull(domain.title)
            assertNotNull(domain.symbol)
            assertEquals(true, domain.title.isNotBlank())
            assertEquals(true, domain.symbol.isNotBlank())
        }
    }

    @Test
    fun `workspace pager indices match domain order`() {
        val domains = WorkspaceDomain.entries.toTypedArray()
        assertEquals(0, domains.indexOf(WorkspaceDomain.PLANNING))
        assertEquals(1, domains.indexOf(WorkspaceDomain.KNOWLEDGE))
        assertEquals(2, domains.indexOf(WorkspaceDomain.HEALTH))
        assertEquals(3, domains.indexOf(WorkspaceDomain.TRAVEL))
        assertEquals(4, domains.indexOf(WorkspaceDomain.HOUSEHOLD))
        assertEquals(5, domains.indexOf(WorkspaceDomain.PEOPLE))
    }

    @Test
    fun `workspace selection state transitions correctly`() {
        val domains = WorkspaceDomain.entries.toTypedArray()
        var pageIndex: Int

        // Swipe up / switch to Knowledge
        pageIndex = 1
        assertEquals(WorkspaceDomain.KNOWLEDGE, domains[pageIndex])

        // Switch back to Planning
        pageIndex = 0
        assertEquals(WorkspaceDomain.PLANNING, domains[pageIndex])

        // Switch to Health
        pageIndex = 2
        assertEquals(WorkspaceDomain.HEALTH, domains[pageIndex])
        assertEquals("Health Workspace", domains[pageIndex].title)
        assertEquals("🩺", domains[pageIndex].symbol)
    }

    @Test
    fun `infinite virtual page index maps correctly via modulo`() {
        val domainCount = WorkspaceDomain.entries.size
        val middle = Int.MAX_VALUE / 2
        val initialPage = middle - (middle % domainCount)

        // Initial centered page resolves to index 0 (Planning)
        assertEquals(0, initialPage % domainCount)
        assertEquals(WorkspaceDomain.PLANNING, WorkspaceDomain.entries[initialPage % domainCount])

        // Swiping 1 page down resolves to index 1 (Knowledge)
        assertEquals(1, (initialPage + 1) % domainCount)
        assertEquals(WorkspaceDomain.KNOWLEDGE, WorkspaceDomain.entries[(initialPage + 1) % domainCount])

        // Swiping 1 page up (wrap around) resolves to index 5 (People)
        assertEquals(5, (initialPage - 1) % domainCount)
        assertEquals(WorkspaceDomain.PEOPLE, WorkspaceDomain.entries[(initialPage - 1) % domainCount])
    }

    @Test
    fun `dot selector click offset calculates nearest target page`() {
        val domainCount = 6
        val currentPhysicalPage = 1_073_741_822 // arbitrary physical page index where physical % 6 == 2 (HEALTH)
        val currentLogicalIndex = currentPhysicalPage % domainCount
        assertEquals(2, currentLogicalIndex)

        // Target: HOUSEHOLD (index 4)
        val targetIndex = 4
        val offset = targetIndex - currentLogicalIndex
        val nextPhysicalPage = currentPhysicalPage + offset

        assertEquals(1_073_741_824, nextPhysicalPage)
        assertEquals(4, nextPhysicalPage % domainCount)
    }
}
