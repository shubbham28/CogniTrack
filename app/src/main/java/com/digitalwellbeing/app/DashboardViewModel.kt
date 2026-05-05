package com.digitalwellbeing.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.digitalwellbeing.ui.DashboardState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface DashboardUiState {
    data object Loading : DashboardUiState
    data object MissingPermission : DashboardUiState
    data class Ready(val state: DashboardState, val status: String) : DashboardUiState
    data class Error(val message: String) : DashboardUiState
}

class DashboardViewModel(
    private val repository: DashboardRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    fun refresh() {
        _uiState.value = if (_uiState.value is DashboardUiState.Ready) _uiState.value else DashboardUiState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = try {
                val dashboard = repository.loadDashboardState()
                DashboardUiState.Ready(dashboard, "Imported live UsageStats from this device")
            } catch (missing: MissingUsageAccessException) {
                DashboardUiState.MissingPermission
            } catch (t: Throwable) {
                DashboardUiState.Error(t.message ?: "Failed to import device activity")
            }
        }
    }
}

class DashboardViewModelFactory(
    private val repository: DashboardRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DashboardViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
