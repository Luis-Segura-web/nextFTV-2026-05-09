package com.stream.iptvrevolut.presentation.screens.profileselector

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stream.iptvrevolut.domain.model.ServerProfile
import com.stream.iptvrevolut.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileSelectorViewModel @Inject constructor(
    private val repository: ProfileRepository
) : ViewModel() {

    private val _profiles = MutableStateFlow<List<ServerProfile>>(emptyList())
    val profiles: StateFlow<List<ServerProfile>> = _profiles.asStateFlow()

    var profileSelected by mutableStateOf(false)
        private set

    init {
        loadProfiles()
    }

    private fun loadProfiles() {
        viewModelScope.launch {
            repository.getProfiles().collect {
                _profiles.value = it
            }
        }
    }

    fun onProfileClick(profile: ServerProfile) {
        viewModelScope.launch {
            repository.setActiveProfile(profile.id)
            profileSelected = true
        }
    }

    fun onDeleteClick(profile: ServerProfile) {
        viewModelScope.launch {
            repository.deleteProfile(profile)
        }
    }
}
