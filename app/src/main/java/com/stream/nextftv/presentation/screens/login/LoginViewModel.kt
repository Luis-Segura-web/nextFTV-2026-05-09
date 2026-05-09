package com.stream.nextftv.presentation.screens.login

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stream.nextftv.domain.repository.ProfileRepository
import com.stream.nextftv.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val repository: ProfileRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val editingProfileId: Int? = savedStateHandle.get<Int>("profileId")?.takeIf { it >= 0 }

    var name by mutableStateOf("")
    var url by mutableStateOf("")
    var username by mutableStateOf("")
    var password by mutableStateOf("")

    var isLoading by mutableStateOf(false)
    var errorResId by mutableStateOf<Int?>(null)
    var loginSuccess by mutableStateOf(false)
    var hasSavedProfiles by mutableStateOf(false)
    var isEditMode by mutableStateOf(editingProfileId != null)
        private set

    init {
        viewModelScope.launch {
            repository.getProfiles().collect { profiles ->
                hasSavedProfiles = profiles.isNotEmpty()
            }
        }

        editingProfileId?.let { profileId ->
            viewModelScope.launch {
                repository.getProfileById(profileId)?.let { profile ->
                    name = profile.name
                    url = profile.url
                    username = profile.username
                    password = profile.password
                }
            }
        }
    }

    fun onLoginClick() {
        if (name.isBlank() || url.isBlank() || username.isBlank() || password.isBlank()) {
            errorResId = R.string.login_error_empty_fields
            return
        }

        viewModelScope.launch {
            isLoading = true
            errorResId = null
            
            val result = editingProfileId?.let {
                repository.updateProfile(it, name, url, username, password)
            } ?: repository.loginAndSave(name, url, username, password)
            
            isLoading = false
            result.onSuccess {
                loginSuccess = true
            }.onFailure { exception ->
                errorResId = when {
                    exception.message?.contains("Credenciales", true) == true -> R.string.login_error_invalid_credentials
                    else -> R.string.login_error_network
                }
            }
        }
    }
}
