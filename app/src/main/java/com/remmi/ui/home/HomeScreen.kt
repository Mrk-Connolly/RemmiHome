package com.remmi.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.remmi.core.host.RemmiHost
import com.remmi.ui.settings.SettingsScreen
import com.remmi.ui.theme.RemmiPreferences
import com.remmi.ui.theme.remmiColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    host: RemmiHost,
    preferences: RemmiPreferences? = null
) {
    var isSettingsOpen by rememberSaveable { mutableStateOf(false) }
    val colors = MaterialTheme.remmiColors

    if (isSettingsOpen) {
        SettingsScreen(
            onBack = { isSettingsOpen = false },
            preferences = preferences
        )
    } else {
        val pagerState = rememberPagerState(
            initialPage = 1,
            pageCount = { HomeDestination.entries.size }
        )
        val snackbarHostState = remember { SnackbarHostState() }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = colors.background,
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    when (HomeDestination.entries[page]) {
                        HomeDestination.WORKSPACES -> WorkspacesScreen()
                        HomeDestination.REMMI_HOME -> RemmiHomeScreen(
                            host = host,
                            snackbarHostState = snackbarHostState,
                            preferences = preferences
                        )
                        HomeDestination.APPS -> AppsScreen(
                            host = host,
                            snackbarHostState = snackbarHostState,
                            preferences = preferences
                        )
                    }
                }

                // Top Overlay Header: 3-Dot Orientation Indicator & Settings
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    LauncherPageIndicator(
                        currentPage = pagerState.currentPage,
                        pageCount = HomeDestination.entries.size,
                        modifier = Modifier.align(Alignment.Center)
                    )

                    IconButton(
                        onClick = { isSettingsOpen = true },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .semantics { contentDescription = "Settings" }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = colors.textSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LauncherPageIndicator(
    currentPage: Int,
    pageCount: Int,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.remmiColors

    Row(
        modifier = modifier.semantics {
            contentDescription = "Page ${currentPage + 1} of $pageCount"
        },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { index ->
            val isSelected = index == currentPage
            val dotColor by animateColorAsState(
                targetValue = if (isSelected) colors.accent else colors.divider,
                label = "DotColorAnimation"
            )

            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(color = dotColor, shape = CircleShape)
            )
        }
    }
}
