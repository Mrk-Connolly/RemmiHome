package com.remmi.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.platform.LocalContext
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
import com.remmi.core.android.launcher.RemmiAppInfo
import com.remmi.core.host.RemmiHost
import com.remmi.ui.theme.RemmiPreferences
import com.remmi.ui.theme.remmiColors
import kotlinx.coroutines.launch

@Composable
fun AppsScreen(
    host: RemmiHost,
    snackbarHostState: SnackbarHostState,
    preferences: RemmiPreferences? = null,
) {
    val colors = MaterialTheme.remmiColors
    val context = LocalContext.current

    val launcherCapability = host.launcherCapability
    val installedApps by (launcherCapability?.installedApps?.collectAsState()
        ?: remember { mutableStateOf(emptyList()) })
    val isDiscovering by (launcherCapability?.isDiscovering?.collectAsState()
        ?: remember { mutableStateOf(value = false) })

    val favoritePackages by (preferences?.favoritePackages?.collectAsState()
        ?: remember { mutableStateOf(emptyList()) })

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf(AppCategory.ALL) }
    var recentAppPackages by rememberSaveable { mutableStateOf(emptyList<String>()) }

    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        if (installedApps.isEmpty()) {
            launcherCapability?.refreshInstalledApps()
        }
    }

    // Filter uninstalled packages from recent apps list automatically
    val recentApps = remember(installedApps, recentAppPackages) {
        recentAppPackages.mapNotNull { pkg ->
            installedApps.find { it.packageName == pkg }
        }
    }

    val categoryFilteredApps = remember(installedApps, selectedCategory, recentApps, context) {
        when (selectedCategory) {
            AppCategory.ALL -> installedApps
            AppCategory.RECENTS -> recentApps
            else -> installedApps.filter { resolveAppCategory(context, it) == selectedCategory }
        }
    }

    val filteredApps = remember(categoryFilteredApps, searchQuery) {
        if (searchQuery.isBlank()) {
            categoryFilteredApps
        } else {
            categoryFilteredApps.filter {
                it.label.contains(searchQuery, ignoreCase = true) ||
                        it.packageName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val handleAppClick: (RemmiAppInfo) -> Unit = { appInfo ->
        focusManager.clearFocus()
        recentAppPackages = (listOf(appInfo.packageName) + recentAppPackages).asSequence().distinct().take(10).toList()
        val success = launcherCapability?.launchApp(appInfo) ?: false
        if (!success) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Unable to launch ${appInfo.label}")
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 56.dp, bottom = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Apps",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                if (installedApps.isNotEmpty()) {
                    Text(
                        text = "${installedApps.size} apps",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textSecondary
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Search applications" },
                placeholder = {
                    Text("Search applications...", color = colors.textMuted)
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
        }

        // Category Filter Chips
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(AppCategory.entries, key = { it.name }) { category ->
                val isSelected = selectedCategory == category
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedCategory = category },
                    label = { Text(category.displayName) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = colors.accent,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = colors.surface,
                        labelColor = colors.textSecondary
                    ),
                    modifier = Modifier.semantics { contentDescription = "Filter by ${category.displayName}" }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            when {
                isDiscovering && installedApps.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            color = colors.accent
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Discovering installed applications...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.textSecondary
                        )
                    }
                }

                filteredApps.isEmpty() && searchQuery.isNotBlank() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "No applications match '$searchQuery'",
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
                }

                filteredApps.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (selectedCategory == AppCategory.RECENTS) "No recently used applications" else "No applications in ${selectedCategory.displayName}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.textSecondary
                        )
                    }
                }

                else -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // "Recently Used" horizontal section when viewing ALL without active search query
                        if ((selectedCategory == AppCategory.ALL) && (searchQuery.isBlank()) && (recentApps.isNotEmpty())) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                            ) {
                                Text(
                                    text = "Recently Used",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textSecondary,
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                                )
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 24.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(recentApps, key = { "recent_${it.packageName}" }) { appInfo ->
                                        RecentAppCard(
                                            appInfo = appInfo,
                                            onAppClick = { handleAppClick(appInfo) }
                                        )
                                    }
                                }
                            }
                        }

                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 100.dp),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filteredApps, key = { "${it.packageName}/${it.activityName}" }) { appInfo ->
                                val isFavorite = favoritePackages.contains(appInfo.packageName)
                                AppLauncherCard(
                                    appInfo = appInfo,
                                    isFavorite = isFavorite,
                                    onAppClick = { handleAppClick(appInfo) },
                                    onToggleFavorite = {
                                        preferences?.toggleFavoritePackage(appInfo.packageName)
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
private fun RecentAppCard(
    appInfo: RemmiAppInfo,
    onAppClick: () -> Unit
) {
    val colors = MaterialTheme.remmiColors

    Card(
        modifier = Modifier
            .width(88.dp)
            .clickable(onClick = onAppClick)
            .semantics {
                role = Role.Button
                contentDescription = "Launch recent ${appInfo.label}"
            },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = colors.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = colors.accentSoft
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = appInfo.label.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
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
}

@Composable
private fun AppLauncherCard(
    appInfo: RemmiAppInfo,
    isFavorite: Boolean,
    onAppClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val colors = MaterialTheme.remmiColors

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onAppClick)
            .semantics {
                role = Role.Button
                contentDescription = "Launch ${appInfo.label}"
            },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = colors.surface
        )
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(48.dp)
                    .semantics {
                        contentDescription = if (isFavorite) "Remove ${appInfo.label} from Favorites" else "Add ${appInfo.label} to Favorites"
                    }
            ) {
                Text(
                    text = if (isFavorite) "★" else "☆",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isFavorite) colors.accent else colors.textMuted
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
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
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = appInfo.label,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    color = colors.textPrimary
                )
            }
        }
    }
}
