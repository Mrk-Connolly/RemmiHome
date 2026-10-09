package com.remmi.ui.theme

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
