package com.remmi.ui.home

import android.content.Context
import com.remmi.core.android.launcher.RemmiAppInfo

/**
 * Represents functional application categories for filtering on the Remmi Apps screen.
 */
enum class AppCategory(val displayName: String, val iconSymbol: String) {
    RECENTS("Recent", "⏱️"),
    ALL("All", "⊞"),
    MESSAGING("Messaging", "💬"),
    MAPS_TRIPS("Maps & Trips", "🗺️"),
    TOOLS("Tools", "⚙️"),
    MUSIC("Music", "🎵"),
    FILES("Files", "📁"),
    SHOPPING("Shopping", "🛒"),
    GAMES("Games", "🎮")
}

/**
 * Deterministically resolves the [AppCategory] for a given [RemmiAppInfo] using package heuristics
 * and pre-resolved category metadata, falling back safely to [AppCategory.TOOLS].
 * Operates purely in-memory without synchronous PackageManager IPC calls on the main thread.
 */
fun resolveAppCategory(context: Context?, appInfo: RemmiAppInfo): AppCategory {
    val packageNameLower = appInfo.packageName.lowercase()

    // Deterministic heuristics based on package names
    when {
        packageNameLower.contains("map") || packageNameLower.contains("waze") || packageNameLower.contains("uber") || packageNameLower.contains("trip") || packageNameLower.contains("transit") || packageNameLower.contains("flight") || packageNameLower.contains("hotel") || packageNameLower.contains("travel") || packageNameLower.contains("navigation") -> return AppCategory.MAPS_TRIPS
        packageNameLower.contains("music") || packageNameLower.contains("spotify") || packageNameLower.contains("audio") || packageNameLower.contains("sound") || packageNameLower.contains("podcast") || packageNameLower.contains("radio") || packageNameLower.contains("player") -> return AppCategory.MUSIC
        packageNameLower.contains("file") || packageNameLower.contains("download") || packageNameLower.contains("storage") || packageNameLower.contains("manager") || packageNameLower.contains("documentsui") || packageNameLower.contains("folder") -> return AppCategory.FILES
        packageNameLower.contains("shop") || packageNameLower.contains("store") || packageNameLower.contains("amazon") || packageNameLower.contains("ebay") || packageNameLower.contains("buy") || packageNameLower.contains("cart") || packageNameLower.contains("market") -> return AppCategory.SHOPPING
        packageNameLower.contains("game") || packageNameLower.contains("play.games") -> return AppCategory.GAMES
        packageNameLower.contains("messag") || packageNameLower.contains("chat") || packageNameLower.contains("whatsapp") || packageNameLower.contains("telegram") || packageNameLower.contains("signal") || packageNameLower.contains("dialer") || packageNameLower.contains("phone") || packageNameLower.contains("contacts") || packageNameLower.contains("mail") || packageNameLower.contains("gmail") || packageNameLower.contains("talk") || packageNameLower.contains("social") -> return AppCategory.MESSAGING
        packageNameLower.contains("doc") || packageNameLower.contains("sheet") || packageNameLower.contains("slide") || packageNameLower.contains("drive") || packageNameLower.contains("calc") || packageNameLower.contains("calendar") || packageNameLower.contains("clock") || packageNameLower.contains("notes") || packageNameLower.contains("setting") || packageNameLower.contains("camera") || packageNameLower.contains("photos") || packageNameLower.contains("gallery") -> return AppCategory.TOOLS
    }

    // Check pre-resolved category metadata if available
    val categoryString = appInfo.category?.lowercase()
    if (categoryString != null) {
        when {
            categoryString.contains("audio") || categoryString.contains("music") -> return AppCategory.MUSIC
            categoryString.contains("video") || categoryString.contains("image") || categoryString.contains("productivity") -> return AppCategory.TOOLS
            categoryString.contains("game") -> return AppCategory.GAMES
            categoryString.contains("map") || categoryString.contains("navigation") || categoryString.contains("travel") -> return AppCategory.MAPS_TRIPS
            categoryString.contains("social") || categoryString.contains("news") || categoryString.contains("message") || categoryString.contains("communication") -> return AppCategory.MESSAGING
        }
    }

    return AppCategory.TOOLS
}
