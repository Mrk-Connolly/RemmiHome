package com.remmi.ui.home

/**
 * Data model representing a user-configurable application category.
 *
 * @property id Unique identifier for the category.
 * @property name Display name of the category (e.g., "Messaging", "Maps & Trips").
 * @property iconSymbol Display icon or emoji symbol (e.g., "💬", "🗺️").
 * @property position Display order position in the category toolbar (0 = 1st, 1 = 2nd, etc.).
 * @property packageNames List of application package names assigned to this category.
 */
data class UserAppCategory(
    val id: String,
    val name: String,
    val iconSymbol: String = "📁",
    val position: Int = 0,
    val packageNames: List<String> = emptyList()
) {
    companion object {
        /**
         * Standard default custom categories initialized on clean installation.
         */
        fun getDefaultCategories(): List<UserAppCategory> {
            return listOf(
                UserAppCategory("cat_messaging", "Messaging", "💬", 0),
                UserAppCategory("cat_maps", "Maps & Trips", "🗺️", 1),
                UserAppCategory("cat_tools", "Tools", "⚙️", 2),
                UserAppCategory("cat_music", "Music", "🎵", 3),
                UserAppCategory("cat_files", "Files", "📁", 4),
                UserAppCategory("cat_shopping", "Shopping", "🛒", 5),
                UserAppCategory("cat_games", "Games", "🎮", 6)
            )
        }
    }
}
