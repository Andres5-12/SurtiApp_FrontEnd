package com.example.surtiapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.surtiapp.data.model.Negocio
import com.example.surtiapp.data.model.Usuario
import com.example.surtiapp.data.network.ApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RegistroViewModel(private val api: ApiService) : ViewModel() {
    private val _uiState = MutableStateFlow<RegistroUiState>(RegistroUiState.Idle)
    val uiState = _uiState.asStateFlow()

    fun registrar(nombreU: String, email: String, pass: String, nombreN: String, tipoN: String, nit: String) {
        viewModelScope.launch {
            _uiState.value = RegistroUiState.Loading
            try {
                // 1. Registro de Usuario
                val user = api.registrarUsuario(Usuario(nombre = nombreU, email = email, password = pass))
                // 2. Creación de Negocio ligado al Usuario
                val negocio = api.crearNegocio(Negocio(
                    nombre = nombreN,
                    tipoNegocio = tipoN,
                    identificacionFiscal = nit,
                    usuario = user
                ))
                _uiState.value = RegistroUiState.Success(user.id ?: 0, negocio.id ?: 0)
            } catch (e: Exception) {
                _uiState.value = RegistroUiState.Error(e.message ?: "Error desconocido")
            }
        }
    }
}

sealed class RegistroUiState {
    object Idle : RegistroUiState()
    object Loading : RegistroUiState()
    data class Success(val userId: Long, val negocioId: Long) : RegistroUiState()
    data class Error(val message: String) : RegistroUiState()
}
