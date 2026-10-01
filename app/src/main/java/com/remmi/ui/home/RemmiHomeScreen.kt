package com.remmi.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.remmi.core.android.launcher.RemmiAppInfo
import com.remmi.core.host.RemmiHost
import com.remmi.ui.search.GlobalSearchResult
import com.remmi.ui.search.filterGlobalSearch
import com.remmi.ui.theme.RemmiPreferences
import com.remmi.ui.theme.remmiColors
import kotlinx.coroutines.launch

@Composable
fun RemmiHomeScreen(
    host: RemmiHost,
    snackbarHostState: SnackbarHostState,
    preferences: RemmiPreferences? = null
) {
    val colors = MaterialTheme.remmiColors
    val launcherCapability = host.launcherCapability
    val installedApps by (launcherCapability?.installedApps?.collectAsState()
        ?: remember { mutableStateOf(emptyList()) })

    val savedFavorites by (preferences?.favoritePackages?.collectAsState()
        ?: remember { mutableStateOf(emptyList()) })

    var searchQuery by rememberSaveable { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        if (installedApps.isEmpty()) {
            launcherCapability?.refreshInstalledApps()
        }
    }

    val favoriteApps = remember(installedApps, savedFavorites) {
        if (savedFavorites.isNotEmpty()) {
            savedFavorites.mapNotNull { pkg -> installedApps.find { it.packageName == pkg } }.take(6)
        } else {
            installedApps.take(6)
        }
    }

    val globalSearchResults = remember(installedApps, savedFavorites, searchQuery) {
        if (searchQuery.isBlank()) {
            emptyList()
        } else {
            filterGlobalSearch(installedApps, savedFavorites, searchQuery)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(top = 56.dp, bottom = 24.dp)
    ) {
        // Search Input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Search applications" },
            placeholder = {
                Text("Search...", color = colors.textMuted)
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = colors.surface,
                unfocusedContainerColor = colors.surface,
                focusedBorderColor = colors.accent,
                unfocusedBorderColor = colors.divider,
                focusedTextColor = colors.textPrimary,
                unfocusedTextColor = colors.textPrimary
            ),
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { searchQuery = "" },
                        modifier = Modifier.semantics { contentDescription = "Clear search" }
                    ) {
                        Text("✕", style = MaterialTheme.typography.titleMedium, color = colors.textSecondary)
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                focusManager.clearFocus()
            })
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (searchQuery.isNotBlank()) {
            // Global Search Results Surface
            Text(
                text = "SEARCH RESULTS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = colors.textMuted,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (globalSearchResults.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No results for '$searchQuery'",
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.textSecondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { searchQuery = "" },
                        modifier = Modifier.semantics { contentDescription = "Clear search filter" }
                    ) {
                        Text("Clear search", color = colors.accent)
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    globalSearchResults.forEach { result ->
                        GlobalSearchResultItem(
                            result = result,
                            onResultClick = {
                                focusManager.clearFocus()
                                val launchCommand = result.toCommand()
                                val success = launcherCapability?.launchApp(launchCommand.appInfo) ?: false
                                if (!success) {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Unable to launch ${launchCommand.appInfo.label}")
                                    }
                                }
                            }
                        )
                    }
                }
            }
        } else {
            // Standard Home View: Reserved Calendar Area + FAVORITE APPS
            Spacer(modifier = Modifier.height(156.dp))

            Text(
                text = "FAVORITE APPS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = colors.textMuted,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (favoriteApps.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No favorite applications",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textMuted
                    )
                }
            } else {
                val rows = favoriteApps.chunked(3)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    rows.forEach { rowApps ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            rowApps.forEach { appInfo ->
                                FavoriteAppItem(
                                    appInfo = appInfo,
                                    onAppClick = {
                                        focusManager.clearFocus()
                                        val success = launcherCapability?.launchApp(appInfo) ?: false
                                        if (!success) {
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("Unable to launch ${appInfo.label}")
                                            }
                                        }
                                    },
                                    onRemoveFavorite = {
                                        preferences?.removeFavoritePackage(appInfo.packageName)
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("Removed ${appInfo.label} from Favorites")
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GlobalSearchResultItem(
    result: GlobalSearchResult,
    onResultClick: () -> Unit
) {
    val colors = MaterialTheme.remmiColors

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onResultClick)
            .semantics {
                role = Role.Button
                contentDescription = "Launch ${result.appInfo.label}"
            },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = colors.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = colors.surfaceSubtle
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = result.appInfo.label.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = result.appInfo.label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = colors.textPrimary
                )
                Text(
                    text = result.appInfo.packageName,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = colors.textMuted
                )
            }
            if (result.isFavorite) {
                Text(
                    text = "★",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.accent,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FavoriteAppItem(
    appInfo: RemmiAppInfo,
    onAppClick: () -> Unit,
    onRemoveFavorite: () -> Unit
) {
    val colors = MaterialTheme.remmiColors

    Column(
        modifier = Modifier
            .size(80.dp)
            .combinedClickable(
                onClick = onAppClick,
                onLongClick = onRemoveFavorite
            )
            .semantics {
                role = Role.Button
                contentDescription = "Launch ${appInfo.label}"
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(48.dp),
            shape = CircleShape,
            color = colors.surfaceSubtle
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = appInfo.label.take(1).uppercase(),
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = appInfo.label,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            color = colors.textPrimary
        )
    }
}
