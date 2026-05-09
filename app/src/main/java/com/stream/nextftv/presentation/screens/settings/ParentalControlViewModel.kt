package com.stream.nextftv.presentation.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.stream.nextftv.data.local.dao.LiveTvDao
import com.stream.nextftv.data.local.dao.ParentalControlDao
import com.stream.nextftv.data.local.dao.SeriesDao
import com.stream.nextftv.data.local.dao.VodDao
import com.stream.nextftv.data.local.entity.ParentalHiddenCategoryEntity
import com.stream.nextftv.domain.model.ServerProfile
import com.stream.nextftv.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ParentalCategoryUiItem(
    val categoryId: String,
    val categoryName: String,
    val isHidden: Boolean
)

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class ParentalControlViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val liveTvDao: LiveTvDao,
    private val vodDao: VodDao,
    private val seriesDao: SeriesDao,
    private val parentalControlDao: ParentalControlDao
) : ViewModel() {
    var hasPin by mutableStateOf(false)
        private set
    var isUnlocked by mutableStateOf(false)
        private set
    var pinError by mutableStateOf<String?>(null)
        private set

    private val activeProfileFlow: StateFlow<ServerProfile?> = profileRepository
        .getProfiles()
        .map { profiles -> profiles.find { it.isActive } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        viewModelScope.launch {
            hasPin = profileRepository.hasParentalPin()
        }
    }

    val liveCategories: StateFlow<List<ParentalCategoryUiItem>> = activeProfileFlow
        .filterNotNull()
        .let { profileFlow ->
            combine(
                profileFlow,
                profileFlow.flatMapLatest { liveTvDao.getAllCategoriesRaw(it.id) },
                profileFlow.flatMapLatest { parentalControlDao.observeHiddenCategoryIds(it.id, "live") }
            ) { _, categories, hidden ->
                val hiddenSet = hidden.toSet()
                categories.map {
                    ParentalCategoryUiItem(
                        categoryId = it.categoryId,
                        categoryName = it.categoryName,
                        isHidden = hiddenSet.contains(it.categoryId)
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val movieCategories: StateFlow<List<ParentalCategoryUiItem>> = activeProfileFlow
        .filterNotNull()
        .let { profileFlow ->
            combine(
                profileFlow,
                profileFlow.flatMapLatest { vodDao.getAllCategoriesRaw(it.id) },
                profileFlow.flatMapLatest { parentalControlDao.observeHiddenCategoryIds(it.id, "vod") }
            ) { _, categories, hidden ->
                val hiddenSet = hidden.toSet()
                categories.map {
                    ParentalCategoryUiItem(
                        categoryId = it.categoryId,
                        categoryName = it.categoryName,
                        isHidden = hiddenSet.contains(it.categoryId)
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val seriesCategories: StateFlow<List<ParentalCategoryUiItem>> = activeProfileFlow
        .filterNotNull()
        .let { profileFlow ->
            combine(
                profileFlow,
                profileFlow.flatMapLatest { seriesDao.getAllCategoriesRaw(it.id) },
                profileFlow.flatMapLatest { parentalControlDao.observeHiddenCategoryIds(it.id, "series") }
            ) { _, categories, hidden ->
                val hiddenSet = hidden.toSet()
                categories.map {
                    ParentalCategoryUiItem(
                        categoryId = it.categoryId,
                        categoryName = it.categoryName,
                        isHidden = hiddenSet.contains(it.categoryId)
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onCategoryVisibilityChange(contentType: String, categoryId: String, visible: Boolean) {
        val profileId = activeProfileFlow.value?.id ?: return
        viewModelScope.launch {
            if (visible) {
                parentalControlDao.showCategory(profileId, contentType, categoryId)
            } else {
                parentalControlDao.hideCategory(
                    ParentalHiddenCategoryEntity(
                        profileId = profileId,
                        contentType = contentType,
                        categoryId = categoryId
                    )
                )
            }
        }
    }

    fun unlockWithPin(pin: String) {
        if (!isValidPin(pin)) {
            pinError = "El NIP debe tener 4 dígitos"
            return
        }
        viewModelScope.launch {
            val ok = profileRepository.verifyParentalPin(pin)
            isUnlocked = ok
            pinError = if (ok) null else "NIP incorrecto"
        }
    }

    fun createPin(pin: String, confirmPin: String) {
        if (!isValidPin(pin) || !isValidPin(confirmPin)) {
            pinError = "El NIP debe tener 4 dígitos"
            return
        }
        if (pin != confirmPin) {
            pinError = "Los NIP no coinciden"
            return
        }
        viewModelScope.launch {
            profileRepository.setParentalPin(pin)
            hasPin = true
            isUnlocked = true
            pinError = null
        }
    }

    fun changePin(
        currentPin: String,
        newPin: String,
        confirmNewPin: String,
        onSuccess: () -> Unit = {}
    ) {
        if (!isValidPin(currentPin) || !isValidPin(newPin) || !isValidPin(confirmNewPin)) {
            pinError = "El NIP debe tener 4 dígitos"
            return
        }
        if (newPin != confirmNewPin) {
            pinError = "Los NIP nuevos no coinciden"
            return
        }
        viewModelScope.launch {
            val currentOk = profileRepository.verifyParentalPin(currentPin)
            if (!currentOk) {
                pinError = "NIP actual incorrecto"
                return@launch
            }
            profileRepository.setParentalPin(newPin)
            pinError = null
            onSuccess()
        }
    }

    fun clearError() {
        pinError = null
    }

    private fun isValidPin(value: String): Boolean = value.matches(Regex("\\d{4}"))
}
