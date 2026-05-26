package com.example.surtiapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.surtiapp.data.model.Producto
import com.example.surtiapp.data.repository.AppRepository
import com.example.surtiapp.data.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed class InventoryUiState {
    object Loading : InventoryUiState()
    data class Success(val productos: List<Producto>) : InventoryUiState()
    data class Error(val message: String) : InventoryUiState()
}

class InventoryViewModel(
    private val repository: AppRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<InventoryUiState>(InventoryUiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        fetchProductos()
    }

    fun fetchProductos() {
        viewModelScope.launch {
            _uiState.value = InventoryUiState.Loading
            try {
                val negocioId = sessionManager.negocioId.first()
                if (negocioId != null) {
                    val result = repository.getProductos(negocioId)
                    result.onSuccess {
                        _uiState.value = InventoryUiState.Success(it)
                    }.onFailure {
                        _uiState.value = InventoryUiState.Error(it.message ?: "Error desconocido")
                    }
                } else {
                    _uiState.value = InventoryUiState.Error("No se encontró ID de negocio")
                }
            } catch (e: Exception) {
                _uiState.value = InventoryUiState.Error(e.localizedMessage ?: "Error de conexión")
            }
        }
    }
}
