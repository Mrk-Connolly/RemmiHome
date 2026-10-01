package com.remmi.ui.home

import android.content.Context
import android.content.pm.ApplicationInfo
import com.remmi.core.android.launcher.RemmiAppInfo

/**
 * Represents functional application categories for filtering on the Remmi Apps screen.
 */
enum class AppCategory(val displayName: String) {
    ALL("All"),
    RECENTS("Recents"),
    COMMUNICATION("Communication"),
    PRODUCTIVITY("Productivity"),
    MEDIA("Media"),
    GAMES("Games"),
    UTILITIES("Utilities"),
    OTHER("Other")
}

/**
 * Deterministically resolves the [AppCategory] for a given [RemmiAppInfo] using package heuristics
 * and Android [ApplicationInfo] category metadata, falling back safely to [AppCategory.OTHER].
 */
fun resolveAppCategory(context: Context?, appInfo: RemmiAppInfo): AppCategory {
    val packageNameLower = appInfo.packageName.lowercase()

    // Deterministic heuristics based on package names
    when {
        packageNameLower.contains("camera") || packageNameLower.contains("photos") || packageNameLower.contains("gallery") -> return AppCategory.MEDIA
        packageNameLower.contains("youtube") || packageNameLower.contains("video") || packageNameLower.contains("music") || packageNameLower.contains("audio") || packageNameLower.contains("player") -> return AppCategory.MEDIA
        packageNameLower.contains("game") || packageNameLower.contains("play.games") -> return AppCategory.GAMES
        packageNameLower.contains("messag") || packageNameLower.contains("chat") || packageNameLower.contains("whatsapp") || packageNameLower.contains("dialer") || packageNameLower.contains("phone") || packageNameLower.contains("contacts") || packageNameLower.contains("mail") || packageNameLower.contains("gmail") -> return AppCategory.COMMUNICATION
        packageNameLower.contains("doc") || packageNameLower.contains("sheet") || packageNameLower.contains("slide") || packageNameLower.contains("drive") || packageNameLower.contains("calculator") || packageNameLower.contains("calendar") || packageNameLower.contains("clock") || packageNameLower.contains("notes") -> return AppCategory.PRODUCTIVITY
        packageNameLower.contains("setting") || packageNameLower.contains("file") || packageNameLower.contains("safety") -> return AppCategory.UTILITIES
    }

    if (context == null) return AppCategory.OTHER

    // Android System ApplicationInfo Category Metadata (API 26+)
    return try {
        val pm = context.packageManager
        val appInfoFlags = pm.getApplicationInfo(appInfo.packageName, 0)
        when (appInfoFlags.category) {
            ApplicationInfo.CATEGORY_AUDIO, ApplicationInfo.CATEGORY_VIDEO, ApplicationInfo.CATEGORY_IMAGE -> AppCategory.MEDIA
            ApplicationInfo.CATEGORY_GAME -> AppCategory.GAMES
            ApplicationInfo.CATEGORY_MAPS -> AppCategory.UTILITIES
            ApplicationInfo.CATEGORY_PRODUCTIVITY -> AppCategory.PRODUCTIVITY
            ApplicationInfo.CATEGORY_SOCIAL, ApplicationInfo.CATEGORY_NEWS -> AppCategory.COMMUNICATION
            else -> AppCategory.OTHER
        }
    } catch (_: Exception) {
        AppCategory.OTHER
    }
}
