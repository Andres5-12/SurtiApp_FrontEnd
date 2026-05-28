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
                    // Primero intentamos con el endpoint directo
                    val responseDirect = try { api.obtenerNegocioPorUsuario(userId) } catch (e: Exception) { null }
                    
                    if (responseDirect != null && responseDirect.isSuccessful && !responseDirect.body().isNullOrEmpty()) {
                        val negocio = responseDirect.body()!![0]
                        sessionManager.saveSession(userId, negocio.id!!)
                        _uiState.value = NegocioUiState.Success(negocio)
                    } else {
                        // FALLBACK: Como el endpoint directo puede no existir en el backend,
                        // buscamos en la lista completa de negocios
                        val responseAll = api.obtenerNegocios()
                        if (responseAll.isSuccessful) {
                            val todos = responseAll.body() ?: emptyList()
                            val miNegocio = todos.find { it.usuario.id == userId }
                            
                            if (miNegocio != null) {
                                sessionManager.saveSession(userId, miNegocio.id!!)
                                _uiState.value = NegocioUiState.Success(miNegocio)
                            } else {
                                _uiState.value = NegocioUiState.NoNegocio(userId)
                            }
                        } else {
                            _uiState.value = NegocioUiState.NoNegocio(userId)
                        }
                    }
                } else {
                    _uiState.value = NegocioUiState.Error("No se encontró una sesión activa")
                }
            } catch (e: Exception) {
                _uiState.value = NegocioUiState.Error("Error de conexión: ${e.message}")
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
                val response = api.crearNegocio(nuevoNegocio)
                if (response.isSuccessful) {
                    val negocioCreado = response.body()!!
                    sessionManager.saveSession(userId, negocioCreado.id!!)
                    _uiState.value = NegocioUiState.Success(negocioCreado)
                } else {
                    _uiState.value = NegocioUiState.Error("Error al registrar negocio: ${response.code()}")
                }
            } catch (e: Exception) {
                _uiState.value = NegocioUiState.Error("Error al registrar: ${e.message}")
            }
        }
    }

    fun actualizarNegocio(id: Long, nombre: String, tipo: String, nit: String, userId: Long) {
        viewModelScope.launch {
            _uiState.value = NegocioUiState.Loading
            try {
                val editado = Negocio(
                    id = id,
                    nombre = nombre,
                    tipoNegocio = tipo,
                    identificacionFiscal = nit,
                    usuario = Usuario(id = userId, nombre = "", email = "")
                )
                val response = api.crearNegocio(editado)
                if (response.isSuccessful) {
                    _uiState.value = NegocioUiState.Success(response.body()!!)
                } else {
                    _uiState.value = NegocioUiState.Error("Error al actualizar: ${response.code()}")
                }
            } catch (e: Exception) {
                _uiState.value = NegocioUiState.Error("Error de conexión: ${e.message}")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            sessionManager.clearSession()
        }
    }
}

sealed class NegocioUiState {
    object Loading : NegocioUiState()
    data class Success(val negocio: Negocio) : NegocioUiState()
    data class NoNegocio(val userId: Long) : NegocioUiState()
    data class Error(val message: String) : NegocioUiState()
}
