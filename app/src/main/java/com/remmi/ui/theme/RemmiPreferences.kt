package com.remmi.ui.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppTheme {
    SYSTEM,
    LIGHT,
    DARK
}

enum class AppAccent(val displayName: String, val hexPreview: Long) {
    BLUE("Blue", 0xFF3978E8),
    GREEN("Green", 0xFF2E7D32),
    PURPLE("Purple", 0xFF7B1FA2),
    ORANGE("Orange", 0xFFE65100),
    RED("Red", 0xFFC62828),
    TEAL("Teal", 0xFF00695C),
    YELLOW("Yellow", 0xFFF57F17),
    PINK("Pink", 0xFFC2185B)
}

class RemmiPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("remmi_preferences", Context.MODE_PRIVATE)

    private val _theme = MutableStateFlow(loadTheme())
    val theme: StateFlow<AppTheme> = _theme.asStateFlow()

    private val _accent = MutableStateFlow(loadAccent())
    val accent: StateFlow<AppAccent> = _accent.asStateFlow()

    private val _favoritePackages = MutableStateFlow(loadFavoritePackages())
    val favoritePackages: StateFlow<List<String>> = _favoritePackages.asStateFlow()

    private fun loadTheme(): AppTheme {
        val saved = prefs.getString(KEY_THEME, AppTheme.SYSTEM.name)
        return try {
            AppTheme.valueOf(saved ?: AppTheme.SYSTEM.name)
        } catch (_: Exception) {
            AppTheme.SYSTEM
        }
    }

    private fun loadAccent(): AppAccent {
        val saved = prefs.getString(KEY_ACCENT, AppAccent.BLUE.name)
        return try {
            AppAccent.valueOf(saved ?: AppAccent.BLUE.name)
        } catch (_: Exception) {
            AppAccent.BLUE
        }
    }

    private fun loadFavoritePackages(): List<String> {
        val savedString = prefs.getString(KEY_FAVORITES, null) ?: return emptyList()
        return savedString.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .take(6)
    }

    fun setTheme(theme: AppTheme) {
        prefs.edit { putString(KEY_THEME, theme.name) }
        _theme.value = theme
    }

    fun setAccent(accent: AppAccent) {
        prefs.edit { putString(KEY_ACCENT, accent.name) }
        _accent.value = accent
    }

    fun setFavoritePackages(packages: List<String>) {
        val cleaned = packages.map { it.trim() }.filter { it.isNotEmpty() }.distinct().take(6)
        val serialized = cleaned.joinToString(",")
        prefs.edit { putString(KEY_FAVORITES, serialized) }
        _favoritePackages.value = cleaned
    }

    fun addFavoritePackage(packageName: String) {
        val current = _favoritePackages.value.toMutableList()
        if (!current.contains(packageName) && current.size < 6) {
            current.add(packageName)
            setFavoritePackages(current)
        }
    }

    fun removeFavoritePackage(packageName: String) {
        val current = _favoritePackages.value.toMutableList()
        if (current.remove(packageName)) {
            setFavoritePackages(current)
        }
    }

    fun toggleFavoritePackage(packageName: String) {
        if (_favoritePackages.value.contains(packageName)) {
            removeFavoritePackage(packageName)
        } else {
            addFavoritePackage(packageName)
        }
    }

    fun isFavoritePackage(packageName: String): Boolean {
        return _favoritePackages.value.contains(packageName)
    }

    companion object {
        private const val KEY_THEME = "app_theme"
        private const val KEY_ACCENT = "app_accent"
        private const val KEY_FAVORITES = "favorite_app_packages"

        @Volatile
        private var instance: RemmiPreferences? = null

        fun getInstance(context: Context): RemmiPreferences {
            return instance ?: synchronized(this) {
                instance ?: RemmiPreferences(context.applicationContext).also { instance = it }
            }
        }
    }
}
