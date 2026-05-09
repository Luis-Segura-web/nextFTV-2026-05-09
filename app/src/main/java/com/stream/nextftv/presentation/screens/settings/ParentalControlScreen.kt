package com.stream.nextftv.presentation.screens.settings

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stream.nextftv.presentation.theme.StreamingBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentalControlScreen(
    onBack: () -> Unit,
    viewModel: ParentalControlViewModel = hiltViewModel()
) {
    val isDark = isSystemInDarkTheme()
    val live by viewModel.liveCategories.collectAsStateWithLifecycle()
    val movies by viewModel.movieCategories.collectAsStateWithLifecycle()
    val series by viewModel.seriesCategories.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    var showChangePinDialog by remember { mutableStateOf(false) }

    val tabs = listOf("TV en Vivo", "Películas", "Series")

    Box(modifier = Modifier.fillMaxSize()) {
        StreamingBackground(isDark = isDark)

        Scaffold(
            topBar = {
                ParentalControlTopBar(
                    canChangePin = viewModel.isUnlocked && viewModel.hasPin,
                    onBack = onBack,
                    onChangePin = { showChangePinDialog = true }
                )
            },
            containerColor = androidx.compose.ui.graphics.Color.Transparent
        ) { innerPadding ->
            if (!viewModel.isUnlocked) {
                PinGate(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    hasPin = viewModel.hasPin,
                    error = viewModel.pinError,
                    onUnlock = { pin -> viewModel.unlockWithPin(pin) },
                    onCreatePin = { pin, confirm -> viewModel.createPin(pin, confirm) },
                    onClearError = viewModel::clearError
                )
                return@Scaffold
            }

            val selectedType = when (selectedTab) {
                0 -> "live"
                1 -> "vod"
                else -> "series"
            }
            val selectedItems = when (selectedTab) {
                0 -> live
                1 -> movies
                else -> series
            }

            ParentalControlContent(
                selectedTab = selectedTab,
                tabs = tabs,
                selectedItems = selectedItems,
                selectedType = selectedType,
                innerPadding = innerPadding,
                onSelectTab = { selectedTab = it },
                onCategoryVisibilityChange = { contentType, categoryId, visible ->
                    viewModel.onCategoryVisibilityChange(contentType, categoryId, visible)
                }
            )

            if (showChangePinDialog) {
                ChangePinDialog(
                    onDismiss = {
                        showChangePinDialog = false
                        viewModel.clearError()
                    },
                    error = viewModel.pinError,
                    onConfirm = { current, new, confirm ->
                        viewModel.changePin(current, new, confirm) {
                            showChangePinDialog = false
                        }
                    }
                )
            }
        }
    }
}
