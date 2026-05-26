package com.example.surtiapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.surtiapp.data.model.Transaccion
import com.example.surtiapp.data.repository.AppRepository
import com.example.surtiapp.data.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed class FinanzasUiState {
    object Loading : FinanzasUiState()
    data class Success(val transacciones: List<Transaccion>) : FinanzasUiState()
    data class Error(val message: String) : FinanzasUiState()
}

class FinanzasViewModel(
    private val repository: AppRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<FinanzasUiState>(FinanzasUiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        fetchTransacciones()
    }

    fun fetchTransacciones() {
        viewModelScope.launch {
            _uiState.value = FinanzasUiState.Loading
            try {
                val negocioId = sessionManager.negocioId.first()
                if (negocioId != null) {
                    val result = repository.getTransacciones(negocioId)
                    result.onSuccess {
                        _uiState.value = FinanzasUiState.Success(it)
                    }.onFailure {
                        _uiState.value = FinanzasUiState.Error(it.message ?: "Error desconocido")
                    }
                } else {
                    _uiState.value = FinanzasUiState.Error("No se encontró ID de negocio")
                }
            } catch (e: Exception) {
                _uiState.value = FinanzasUiState.Error(e.localizedMessage ?: "Error de conexión")
            }
        }
    }
}
