package com.gatemaster.app.core.model

/**
 * How the app decides between the light and dark palettes.
 *
 * This exists as an explicit setting rather than always following the system
 * because reading is different from using an app: plenty of people keep their
 * phone in dark mode all day and still want study notes on a light page.
 */
enum class ThemeMode(val key: String, val label: String) {
    SYSTEM("system", "Match system"),
    LIGHT("light", "Light"),
    DARK("dark", "Dark"),
    ;

    companion object {
        fun fromKey(key: String?): ThemeMode =
            entries.firstOrNull { it.key == key } ?: SYSTEM
    }
}
