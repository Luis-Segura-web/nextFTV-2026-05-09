package com.stream.nextftv.presentation.screens.home

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.stream.nextftv.presentation.theme.StreamingBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToSection: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val isDark = isSystemInDarkTheme()
    val activeProfile = viewModel.activeProfile
    val accountStatus = viewModel.liveAccountStatus ?: activeProfile?.accountStatus
    val expirationDate = viewModel.liveExpirationDate ?: activeProfile?.expirationDate
    val isGlobalSyncing = viewModel.isGlobalSyncing
    val counts = viewModel.counts

    val isAccountActive = accountStatus?.equals("Active", ignoreCase = true) == true

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.validateAccountOnHomeEnter()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        StreamingBackground(isDark = isDark)

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            topBar = {
                HomeTopBar(
                    activeProfileName = activeProfile?.name,
                    isGlobalSyncing = isGlobalSyncing,
                    onSyncAll = { viewModel.syncAll() },
                    onOpenSettings = onNavigateToSettings
                )
            },
            bottomBar = {
                HomeBottomBar(
                    expirationDate = expirationDate,
                    accountStatus = accountStatus,
                    isAccountActive = isAccountActive
                )
            }
        ) { innerPadding ->
            HomeContent(
                backgroundBrush = null,
                isAccountActive = isAccountActive,
                isGlobalSyncing = isGlobalSyncing,
                counts = counts,
                syncStates = viewModel.syncStates,
                syncProgress = viewModel.syncProgress,
                syncMessages = viewModel.syncMessages,
                lastSync = viewModel.lastSync,
                innerPadding = innerPadding,
                onSyncModule = viewModel::syncModule,
                onNavigateToSection = onNavigateToSection
            )
        }
    }
}
