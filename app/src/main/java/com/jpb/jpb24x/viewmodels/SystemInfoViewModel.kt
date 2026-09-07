package com.jpb.jpb24x.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jpb.jpb24x.helpers.SocInfo
import com.jpb.jpb24x.helpers.SocRepository
import com.jpb.jpb24x.providers.SocHardwareProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface SocUiState {
    object Loading : SocUiState
    data class Success(val info: SocInfo) : SocUiState
    data class Unknown(val identifiedHardware: List<String>) : SocUiState
}

class SystemInfoViewModel(
    private val repository: SocRepository,
    private val hardwareProvider: SocHardwareProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow<SocUiState>(SocUiState.Loading)
    val uiState: StateFlow<SocUiState> = _uiState

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = SocUiState.Loading
            val match = repository.resolveDeviceSoc()
            if (match != null) {
                _uiState.value = SocUiState.Success(match)
            } else {
                val attemptedIds = hardwareProvider.getPossibleSocIds()
                _uiState.value = SocUiState.Unknown(attemptedIds)
            }
        }
    }
}
