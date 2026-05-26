package com.example.surtiapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.surtiapp.data.model.Negocio
import com.example.surtiapp.data.model.Usuario
import com.example.surtiapp.data.network.ApiService
import com.example.surtiapp.data.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class NegocioViewModel(
    private val api: ApiService,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<NegocioUiState>(NegocioUiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        verificarNegocio()
    }

    fun verificarNegocio() {
        viewModelScope.launch {
            _uiState.value = NegocioUiState.Loading
            try {
                val userId = sessionManager.userId.first()
                if (userId != null) {
                    try {
                        val negocio = api.obtenerNegocioPorUsuario(userId)
                        // Si lo encuentra, actualizamos sesión por si acaso y mostramos perfil
                        sessionManager.saveSession(userId, negocio.id!!)
                        _uiState.value = NegocioUiState.Success(negocio)
                    } catch (e: Exception) {
                        // Si el error es 404 o similar (no encontrado), pasamos a creación
                        _uiState.value = NegocioUiState.NoNegocio(userId)
                    }
                } else {
                    _uiState.value = NegocioUiState.Error("Usuario no autenticado")
                }
            } catch (e: Exception) {
                _uiState.value = NegocioUiState.Error(e.message ?: "Error desconocido")
            }
        }
    }

    fun registrarNegocio(nombre: String, tipo: String, nit: String, userId: Long) {
        viewModelScope.launch {
            _uiState.value = NegocioUiState.Loading
            try {
                val nuevoNegocio = Negocio(
                    nombre = nombre,
                    tipoNegocio = tipo,
                    identificacionFiscal = nit,
                    usuario = Usuario(id = userId, nombre = "", email = "") // Solo necesitamos el ID
                )
                val negocioCreado = api.crearNegocio(nuevoNegocio)
                sessionManager.saveSession(userId, negocioCreado.id!!)
                _uiState.value = NegocioUiState.Success(negocioCreado)
            } catch (e: Exception) {
                _uiState.value = NegocioUiState.Error("Error al registrar: ${e.message}")
            }
        }
    }
}

sealed class NegocioUiState {
    object Loading : NegocioUiState()
    data class Success(val negocio: Negocio) : NegocioUiState()
    data class NoNegocio(val userId: Long) : NegocioUiState()
    data class Error(val message: String) : NegocioUiState()
}
