package com.stream.nextftv.presentation.screens.splash

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stream.nextftv.domain.repository.ProfileRepository
import com.stream.nextftv.presentation.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val repository: ProfileRepository
) : ViewModel() {

    var destination by mutableStateOf<String?>(null)
        private set

    init {
        checkDestination()
    }

    private fun checkDestination() {
        viewModelScope.launch {
            // Delay mínimo para mostrar el logo (2 segundos)
            val delayJob = launch { delay(2000) }
            
            val profiles = repository.getProfiles().first()
            
            delayJob.join()

            destination = when {
                profiles.isEmpty() -> Screen.Login.createRoute()
                profiles.size == 1 -> {
                    repository.setActiveProfile(profiles.first().id)
                    Screen.Home.route
                }
                else -> Screen.ProfileSelector.route
            }
        }
    }
}
