package com.remmi.ui.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import com.remmi.core.android.launcher.RemmiAppInfo
import com.remmi.core.android.launcher.RemmiLauncherCapability
import com.remmi.core.host.RemmiHost
import com.remmi.ui.theme.RemmiTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class HomeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private class TestLauncherCapability : RemmiLauncherCapability {
        private val _installedApps = MutableStateFlow<List<RemmiAppInfo>>(
            listOf(
                RemmiAppInfo("com.example.browser", "BrowserActivity", "Browser"),
                RemmiAppInfo("com.example.camera", "CameraActivity", "Camera"),
                RemmiAppInfo("com.example.calculator", "CalcActivity", "Calculator")
            )
        )
        override val installedApps: StateFlow<List<RemmiAppInfo>> = _installedApps.asStateFlow()

        private val _isDiscovering = MutableStateFlow(false)
        override val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()

        var launchedApp: RemmiAppInfo? = null

        override suspend fun refreshInstalledApps() {
            // No-op for test
        }

        override fun launchApp(appInfo: RemmiAppInfo): Boolean {
            launchedApp = appInfo
            return true
        }
    }

    @Test
    fun homeScreen_defaultsToRemmiHomeDestination() {
        val launcher = TestLauncherCapability()
        val host = RemmiHost(launcherCapability = launcher)

        composeTestRule.setContent {
            RemmiTheme {
                HomeScreen(host = host)
            }
        }

        composeTestRule.onNodeWithContentDescription("Search applications").assertIsDisplayed()
        composeTestRule.onNodeWithText("Browser").assertIsDisplayed()
        composeTestRule.onNodeWithText("Camera").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Page 2 of 3").assertIsDisplayed()
    }

    @Test
    fun homeScreen_swipingRightShowsWorkspacesDestination() {
        val launcher = TestLauncherCapability()
        val host = RemmiHost(launcherCapability = launcher)

        composeTestRule.setContent {
            RemmiTheme {
                HomeScreen(host = host)
            }
        }

        composeTestRule.onNodeWithText("Browser").performTouchInput { swipeRight() }

        composeTestRule.onNodeWithText("Workspaces").assertIsDisplayed()
        composeTestRule.onNodeWithText("Planning").assertIsDisplayed()
        composeTestRule.onNodeWithText("Travel").assertIsDisplayed()
    }

    @Test
    fun homeScreen_swipingLeftShowsAppsDestinationAndDisplaysApps() {
        val launcher = TestLauncherCapability()
        val host = RemmiHost(launcherCapability = launcher)

        composeTestRule.setContent {
            RemmiTheme {
                HomeScreen(host = host)
            }
        }

        composeTestRule.onNodeWithText("Browser").performTouchInput { swipeLeft() }

        composeTestRule.onNodeWithText("Browser").assertIsDisplayed()
        composeTestRule.onNodeWithText("Camera").assertIsDisplayed()
        composeTestRule.onNodeWithText("Calculator").assertIsDisplayed()
    }

    @Test
    fun homeScreen_appsCategoryFiltering_filtersAppsByCategory() {
        val launcher = TestLauncherCapability()
        val host = RemmiHost(launcherCapability = launcher)

        composeTestRule.setContent {
            RemmiTheme {
                HomeScreen(host = host)
            }
        }

        composeTestRule.onNodeWithText("Browser").performTouchInput { swipeLeft() }
        composeTestRule.onNodeWithContentDescription("Filter by Tools").performClick()

        composeTestRule.onNodeWithText("Calculator").assertIsDisplayed()
    }

    @Test
    fun homeScreen_selectingFavoriteAppOnHome_triggersLaunch() {
        val launcher = TestLauncherCapability()
        val host = RemmiHost(launcherCapability = launcher)

        composeTestRule.setContent {
            RemmiTheme {
                HomeScreen(host = host)
            }
        }

        composeTestRule.onNodeWithContentDescription("Launch Camera").performClick()

        assertEquals("com.example.camera", launcher.launchedApp?.packageName)
    }

    @Test
    fun homeScreen_clickingSettingsOnAppsPage_navigatesToSettingsScreen() {
        val launcher = TestLauncherCapability()
        val host = RemmiHost(launcherCapability = launcher)

        composeTestRule.setContent {
            RemmiTheme {
                HomeScreen(host = host)
            }
        }

        composeTestRule.onNodeWithText("Browser").performTouchInput { swipeLeft() }
        composeTestRule.onNodeWithContentDescription("Settings").performClick()

        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Back").assertIsDisplayed()
    }

    @Test
    fun settingsScreen_clickingBack_returnsToHomeScreen() {
        val launcher = TestLauncherCapability()
        val host = RemmiHost(launcherCapability = launcher)

        composeTestRule.setContent {
            RemmiTheme {
                HomeScreen(host = host)
            }
        }

        composeTestRule.onNodeWithText("Browser").performTouchInput { swipeLeft() }
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.onNodeWithContentDescription("Back").performClick()

        composeTestRule.onNodeWithContentDescription("Search applications").assertIsDisplayed()
    }

    @Test
    fun homeScreen_globalSearchOnHome_displaysRankedResultsAndLaunchesApp() {
        val launcher = TestLauncherCapability()
        val host = RemmiHost(launcherCapability = launcher)

        composeTestRule.setContent {
            RemmiTheme {
                HomeScreen(host = host)
            }
        }

        composeTestRule.onNodeWithContentDescription("Search applications").performTextInput("cam")

        composeTestRule.onNodeWithText("SEARCH RESULTS").assertIsDisplayed()
        composeTestRule.onNodeWithText("Camera").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Launch Camera").performClick()

        assertEquals("com.example.camera", launcher.launchedApp?.packageName)
    }

    @Test
    fun homeScreen_globalSearchOnHome_clearsSearchQueryOnLaunch() {
        val launcher = TestLauncherCapability()
        val host = RemmiHost(launcherCapability = launcher)

        composeTestRule.setContent {
            RemmiTheme {
                HomeScreen(host = host)
            }
        }

        composeTestRule.onNodeWithContentDescription("Search applications").performTextInput("cam")
        composeTestRule.onNodeWithText("SEARCH RESULTS").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Launch Camera").performClick()

        assertEquals("com.example.camera", launcher.launchedApp?.packageName)
        composeTestRule.onNodeWithContentDescription("Search applications").assertIsDisplayed()
        composeTestRule.onNodeWithText("SEARCH RESULTS").assertDoesNotExist()
    }

    @Test
    fun homeScreen_homeRequestedTrigger_returnsToHomeAndClosesSettings() {
        val launcher = TestLauncherCapability()
        val host = RemmiHost(launcherCapability = launcher)

        var trigger by mutableStateOf(0)

        composeTestRule.setContent {
            RemmiTheme {
                HomeScreen(host = host, homeRequestedTrigger = trigger)
            }
        }

        composeTestRule.onNodeWithText("Browser").performTouchInput { swipeLeft() }
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.onNodeWithText("Settings").assertIsDisplayed()

        trigger += 1

        composeTestRule.onNodeWithContentDescription("Search applications").assertIsDisplayed()
        composeTestRule.onNodeWithText("Appearance").assertDoesNotExist()
    }

    @Test
    fun homeScreen_clickingAiVoiceButton_opensAiVoiceModal() {
        val launcher = TestLauncherCapability()
        val host = RemmiHost(launcherCapability = launcher)

        composeTestRule.setContent {
            RemmiTheme {
                HomeScreen(host = host)
            }
        }

        composeTestRule.onNodeWithContentDescription("AI Voice Assistant").performClick()

        composeTestRule.onNodeWithText("AI Voice Assistant").assertIsDisplayed()
        composeTestRule.onNodeWithText("Close").performClick()
        composeTestRule.onNodeWithText("AI Voice Assistant").assertDoesNotExist()
    }
}
