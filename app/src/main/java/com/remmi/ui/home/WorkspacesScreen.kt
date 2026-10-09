package com.remmi.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.remmi.core.host.RemmiHost
import com.remmi.plugins.health.ui.HealthWorkspaceScreen
import com.remmi.plugins.knowledge.ui.KnowledgeWorkspaceScreen
import com.remmi.plugins.planning.ui.PlanningWorkspaceScreen
import com.remmi.ui.theme.remmiColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class WorkspaceDomain(
    val title: String,
    val subtitle: String,
    val symbol: String
) {
    PLANNING("Planning Workspace", "Tasks, schedules, & goals", "📅"),
    KNOWLEDGE("Knowledge Workspace", "Lectures & recordings", "🎓"),
    HEALTH("Health Workspace", "Wellbeing & personal insight", "🩺"),
    TRAVEL("Travel Workspace", "Itineraries & bookings", "✈️"),
    HOUSEHOLD("Household Workspace", "Home management, stock, recipes, & shopping", "🏠"),
    PEOPLE("People Workspace", "Contacts & relationships", "👥")
}

@Composable
fun WorkspacesScreen(
    host: RemmiHost? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val foodRepository = remember(context) {
        com.remmi.plugins.food.repository.FoodRepository(storageDir = context.filesDir).apply { loadFromStorage() }
    }
    val domains = remember { WorkspaceDomain.entries.toTypedArray() }
    val domainCount = domains.size

    val initialPage = remember(domainCount) {
        val middle = Int.MAX_VALUE / 2
        middle - (middle % domainCount)
    }

    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { Int.MAX_VALUE }
    )
    val coroutineScope = rememberCoroutineScope()

    var isIndicatorVisible by remember { mutableStateOf(false) }

    LaunchedEffect(pagerState.isScrollInProgress) {
        if (pagerState.isScrollInProgress) {
            isIndicatorVisible = true
        } else {
            delay(1000L)
            isIndicatorVisible = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        // Full-screen Vertical Workspace Pager (Bidirectional Infinite Loop)
        VerticalPager(
            state = pagerState,
            beyondViewportPageCount = 1,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 40.dp)
        ) { page ->
            val logicalIndex = page % domainCount
            when (domains[logicalIndex]) {
                WorkspaceDomain.PLANNING -> PlanningWorkspaceScreen(host = host)
                WorkspaceDomain.KNOWLEDGE -> KnowledgeWorkspaceScreen(host = host)
                WorkspaceDomain.HEALTH -> HealthWorkspaceScreen(host = host)
                WorkspaceDomain.TRAVEL -> GenericWorkspacePlaceholder("Travel Workspace", "Itineraries & bookings")
                WorkspaceDomain.HOUSEHOLD -> com.remmi.plugins.food.ui.HouseholdWorkspaceScreen(foodRepository = foodRepository)
                WorkspaceDomain.PEOPLE -> GenericWorkspacePlaceholder("People Workspace", "Contacts & relationships")
            }
        }

        // Auto-Hiding Vertical Dot Selector on the LEFT
        val currentLogicalIndex = pagerState.currentPage % domainCount
        val currentDomain = domains[currentLogicalIndex]

        AnimatedVisibility(
            visible = isIndicatorVisible,
            enter = fadeIn(animationSpec = tween(200)),
            exit = fadeOut(animationSpec = tween(300)),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 8.dp, top = 40.dp)
        ) {
            FloatingWorkspaceDotSelector(
                selectedDomain = currentDomain,
                onSelectDomain = { domain ->
                    val targetIndex = domains.indexOf(domain)
                    if (targetIndex != -1) {
                        val currentPhysical = pagerState.currentPage
                        val offset = targetIndex - (currentPhysical % domainCount)
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(currentPhysical + offset)
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun FloatingWorkspaceDotSelector(
    selectedDomain: WorkspaceDomain,
    onSelectDomain: (WorkspaceDomain) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.remmiColors

    Surface(
        modifier = modifier.clip(RoundedCornerShape(20.dp)),
        color = colors.surface.copy(alpha = 0.85f),
        tonalElevation = 2.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            WorkspaceDomain.entries.forEach { domain ->
                val isSelected = selectedDomain == domain
                val dotSize by animateDpAsState(
                    targetValue = if (isSelected) 8.dp else 6.dp,
                    label = "DotSizeAnimation"
                )
                val dotColor by animateColorAsState(
                    targetValue = if (isSelected) colors.accent else colors.textMuted.copy(alpha = 0.6f),
                    label = "DotColorAnimation"
                )

                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .clickable { onSelectDomain(domain) }
                        .semantics { contentDescription = "Switch to ${domain.title}" },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(dotSize)
                            .background(color = dotColor, shape = CircleShape)
                    )
                }
            }
        }
    }
}

@Composable
private fun GenericWorkspacePlaceholder(title: String, subtitle: String) {
    val colors = MaterialTheme.remmiColors
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = colors.surface.copy(alpha = 0.85f)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary
                )
            }
        }
    }
}
