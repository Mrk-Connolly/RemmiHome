package com.remmi.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.remmi.core.RemmiApplication
import com.remmi.core.host.RemmiHost
import com.remmi.ui.home.HomeScreen
import com.remmi.ui.theme.RemmiPreferences
import com.remmi.ui.theme.RemmiTheme

@Composable
fun RemmiApp(
    host: RemmiHost? = null,
    homeRequestedTrigger: Int = 0
) {
    val context = LocalContext.current
    val effectiveHost = host ?: (context.applicationContext as? RemmiApplication)?.host ?: RemmiHost()

    val preferences = remember(context) { RemmiPreferences.getInstance(context) }
    val currentTheme by preferences.theme.collectAsState()
    val currentAccent by preferences.accent.collectAsState()

    RemmiTheme(
        appTheme = currentTheme,
        appAccent = currentAccent
    ) {
        HomeScreen(
            host = effectiveHost,
            preferences = preferences,
            homeRequestedTrigger = homeRequestedTrigger
        )
    }
}
