package com.stream.nextftv.presentation.screens.login

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onBackToProfiles: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val focusManager = LocalFocusManager.current

    LaunchedEffect(viewModel.loginSuccess) {
        if (viewModel.loginSuccess) onLoginSuccess()
    }

    LoginContent(
        name = viewModel.name,
        url = viewModel.url,
        username = viewModel.username,
        password = viewModel.password,
        isLoading = viewModel.isLoading,
        isEditMode = viewModel.isEditMode,
        hasSavedProfiles = viewModel.hasSavedProfiles,
        errorText = viewModel.errorResId?.let { stringResource(it) },
        onNameChange = { viewModel.name = it },
        onUrlChange = { viewModel.url = it.sanitize() },
        onUsernameChange = { viewModel.username = it.sanitize() },
        onPasswordChange = { viewModel.password = it.sanitize() },
        onLoginClick = viewModel::onLoginClick,
        onBackToProfiles = onBackToProfiles,
        focusManager = focusManager
    )
}

fun String.sanitize(): String {
    return this.replace("\\s".toRegex(), "").trim()
}
