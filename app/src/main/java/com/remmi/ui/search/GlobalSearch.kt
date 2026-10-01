package com.remmi.ui.search

import com.remmi.core.android.launcher.RemmiAppInfo
import com.remmi.core.eventBus.commands.LaunchAppCommand

/**
 * Represents the match quality of a search result.
 */
enum class MatchType {
    EXACT,
    PREFIX,
    PARTIAL
}

/**
 * Represents a single search result in Remmi Global Search ("What is this?").
 */
data class GlobalSearchResult(
    val appInfo: RemmiAppInfo,
    val isFavorite: Boolean,
    val matchType: MatchType
) {
    /**
     * Converts a search discovery result into an explicit system action command ("What should Remmi do?").
     */
    fun toCommand(): LaunchAppCommand = LaunchAppCommand(appInfo)
}

/**
 * Evaluates and filters installed applications against a search query using deterministic match ranking:
 * 1. Exact name match (case-insensitive)
 * 2. Prefix name match (label starts with query)
 * 3. Partial name match / package name match (label or package contains query)
 * 4. Alphabetical tie-breaker
 *
 * Returns an empty list when [query] is blank.
 */
fun filterGlobalSearch(
    installedApps: List<RemmiAppInfo>,
    favoritePackages: List<String>,
    query: String
): List<GlobalSearchResult> {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return emptyList()

    val queryLower = trimmed.lowercase()

    val results = installedApps.mapNotNull { appInfo ->
        val labelLower = appInfo.label.lowercase()
        val pkgLower = appInfo.packageName.lowercase()

        val matchType = when {
            labelLower == queryLower -> MatchType.EXACT
            labelLower.startsWith(queryLower) -> MatchType.PREFIX
            labelLower.contains(queryLower) || pkgLower.contains(queryLower) -> MatchType.PARTIAL
            else -> null
        }

        if (matchType != null) {
            GlobalSearchResult(
                appInfo = appInfo,
                isFavorite = favoritePackages.contains(appInfo.packageName),
                matchType = matchType
            )
        } else null
    }

    return results.sortedWith(
        compareBy<GlobalSearchResult> { result ->
            when (result.matchType) {
                MatchType.EXACT -> 0
                MatchType.PREFIX -> 1
                MatchType.PARTIAL -> 2
            }
        }.thenBy { it.appInfo.label.lowercase() }
    )
}
