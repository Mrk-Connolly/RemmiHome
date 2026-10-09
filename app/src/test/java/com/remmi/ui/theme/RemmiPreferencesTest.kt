package com.remmi.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class RemmiPreferencesTest {

    private fun parseFavoritePackages(serialized: String?): List<String> {
        if (serialized.isNullOrBlank()) return emptyList()
        return serialized.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .take(20)
    }

    private fun serializeFavoritePackages(packages: List<String>): String {
        return packages
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .take(20)
            .joinToString(",")
    }

    @Test
    fun favoritePackages_serializationAndDeserialization_roundTripsCorrectly() {
        val original = listOf("com.example.camera", "com.example.browser", "com.example.notes")
        val serialized = serializeFavoritePackages(original)

        assertEquals("com.example.camera,com.example.browser,com.example.notes", serialized)

        val deserialized = parseFavoritePackages(serialized)
        assertEquals(original, deserialized)
    }

    @Test
    fun favoritePackages_deduplicatesAndCapsAtTwenty() {
        val input = (1..25).map { "com.example.app$it" }

        val serialized = serializeFavoritePackages(input)
        val deserialized = parseFavoritePackages(serialized)

        assertEquals(20, deserialized.size)
        assertEquals("com.example.app1", deserialized.first())
        assertEquals("com.example.app20", deserialized.last())
    }

    @Test
    fun favoritePackages_handlesEmptyAndNullStrings() {
        assertEquals(emptyList<String>(), parseFavoritePackages(null))
        assertEquals(emptyList<String>(), parseFavoritePackages(""))
        assertEquals(emptyList<String>(), parseFavoritePackages("  , , "))
    }

    private fun parseFavoriteGridSlots(serialized: String?): List<String?> {
        if (serialized == null) return List(20) { null }
        val parts = serialized.split(",")
        val result = MutableList<String?>(20) { null }
        for (i in 0 until minOf(parts.size, 20)) {
            val item = parts[i].trim()
            result[i] = if (item.isNotEmpty()) item else null
        }
        return result
    }

    private fun serializeFavoriteGridSlots(slots: List<String?>): String {
        val fixedSlots = List(20) { index -> slots.getOrNull(index)?.trim()?.ifEmpty { null } }
        return fixedSlots.map { it ?: "" }.joinToString(",")
    }

    @Test
    fun favoriteGridSlots_preservesEmptySlotsAtSpecificIndices() {
        val slots = listOf("com.example.app1", null, "com.example.app2", null, "com.example.app3")
        val serialized = serializeFavoriteGridSlots(slots)

        val parsed = parseFavoriteGridSlots(serialized)
        assertEquals(20, parsed.size)
        assertEquals("com.example.app1", parsed[0])
        assertEquals(null, parsed[1])
        assertEquals("com.example.app2", parsed[2])
        assertEquals(null, parsed[3])
        assertEquals("com.example.app3", parsed[4])
        assertEquals(null, parsed[5])
    }

    @Test
    fun favoriteGridSlots_migratesLegacyPackagesList() {
        val legacy = listOf("com.example.app1", "com.example.app2")
        val migrated = MutableList<String?>(20) { null }
        for (i in legacy.indices) {
            migrated[i] = legacy[i]
        }

        assertEquals("com.example.app1", migrated[0])
        assertEquals("com.example.app2", migrated[1])
        assertEquals(null, migrated[2])
    }
}
