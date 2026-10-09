package com.remmi.ui.accounts

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.remmi.core.android.google.auth.DefaultGoogleAuthCapability
import com.remmi.core.android.google.calendar.DefaultGoogleCalendarCapability
import com.remmi.core.host.RemmiHost
import com.remmi.plugins.planning.calendar.repository.CalendarEventRepository
import com.remmi.ui.theme.remmiColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    host: RemmiHost,
    onBack: () -> Unit
) {
    BackHandler(enabled = true, onBack = onBack)

    val context = LocalContext.current
    val colors = MaterialTheme.remmiColors
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val authCapability = remember(host) {
        host.googleAuthCapability ?: DefaultGoogleAuthCapability()
    }
    val calendarCapability = remember(host) {
        host.calendarCapability ?: DefaultGoogleCalendarCapability(authCapability = authCapability)
    }

    val currentAccount by authCapability.currentAccount.collectAsState()
    var isSyncingCalendar by remember { mutableStateOf(false) }

    val calendarRepository = remember(context) {
        CalendarEventRepository(storageDir = context.filesDir).apply { loadFromStorage() }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = colors.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Accounts & Integration",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.semantics { contentDescription = "Back" }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = colors.textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Section 1: Registered Accounts
            Text(
                text = "Registered Accounts",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = colors.accent,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (currentAccount != null) {
                        val account = currentAccount!!
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = colors.accent.copy(alpha = 0.2f),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "Google Account Profile",
                                        tint = colors.accent
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = account.displayName ?: "Google User",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = account.email,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = colors.accentSoft
                            ) {
                                Text(
                                    text = "Active ✓",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.accent,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            OutlinedButton(
                                onClick = {
                                    coroutineScope.launch {
                                        authCapability.signOut()
                                        snackbarHostState.showSnackbar("Signed out of Google Account")
                                    }
                                }
                            ) {
                                Text("Sign Out", color = colors.accent)
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No Google Account Registered",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Sign in to register your Google Account and sync Google Calendar & tools.",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        val success = authCapability.signIn()
                                        if (success) {
                                            val account = authCapability.currentAccount.value
                                            if (account != null) {
                                                calendarRepository.syncWithGoogleCalendar(
                                                    account.accountId,
                                                    calendarCapability
                                                )
                                            }
                                            snackbarHostState.showSnackbar("Successfully signed in and registered account!")
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Sign in Google Account",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text("Register Google Account")
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section 2: Integrated Google Tools & Services
            Text(
                text = "Google Services & Tool Integrations",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = colors.accent,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Google Calendar Integration Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Google Calendar Sync",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (currentAccount != null) "Direct sync active with Remmi Calendar" else "Sign in required to sync",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary
                            )
                        }

                        Button(
                            onClick = {
                                val account = currentAccount
                                if (account != null) {
                                    isSyncingCalendar = true
                                    coroutineScope.launch {
                                        val synced = calendarRepository.syncWithGoogleCalendar(
                                            account.accountId,
                                            calendarCapability
                                        )
                                        isSyncingCalendar = false
                                        snackbarHostState.showSnackbar("Synced ${synced.size} Google Calendar events!")
                                    }
                                } else {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Please sign in to Google Account first")
                                    }
                                }
                            },
                            enabled = currentAccount != null && !isSyncingCalendar,
                            colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Sync Calendar",
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(if (isSyncingCalendar) "Syncing..." else "Sync Now")
                            }
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 14.dp),
                        color = colors.divider
                    )

                    // Google Tasks & Reminders Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Google Tasks & Agendas",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Automated local-first task sync",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = colors.accentSoft
                        ) {
                            Text(
                                text = "Connected ✓",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.accent,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
