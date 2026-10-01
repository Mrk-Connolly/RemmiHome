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
            .take(6)
    }

    private fun serializeFavoritePackages(packages: List<String>): String {
        return packages
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .take(6)
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
    fun favoritePackages_deduplicatesAndCapsAtSix() {
        val input = listOf(
            "com.example.app1",
            "com.example.app2",
            "com.example.app1", // duplicate
            "com.example.app3",
            "com.example.app4",
            "com.example.app5",
            "com.example.app6",
            "com.example.app7"  // excess
        )

        val serialized = serializeFavoritePackages(input)
        val deserialized = parseFavoritePackages(serialized)

        assertEquals(6, deserialized.size)
        assertEquals(listOf("com.example.app1", "com.example.app2", "com.example.app3", "com.example.app4", "com.example.app5", "com.example.app6"), deserialized)
    }

    @Test
    fun favoritePackages_handlesEmptyAndNullStrings() {
        assertEquals(emptyList<String>(), parseFavoritePackages(null))
        assertEquals(emptyList<String>(), parseFavoritePackages(""))
        assertEquals(emptyList<String>(), parseFavoritePackages("  , , "))
    }
}
