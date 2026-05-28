package com.example.surtiapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.surtiapp.data.model.LoginRequest
import com.example.surtiapp.data.model.Usuario
import com.example.surtiapp.data.network.ApiService
import com.example.surtiapp.data.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LoginViewModel(
    private val api: ApiService,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState = _uiState.asStateFlow()

    fun login(email: String, pass: String) {
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _uiState.value = LoginUiState.Error("Correo electrónico no válido")
            return
        }
        if (pass.length < 6) {
            _uiState.value = LoginUiState.Error("La contraseña debe tener al menos 6 caracteres")
            return
        }

        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            try {
                val request = LoginRequest(email, pass)
                val responseLogin = api.login(request)
                
                if (responseLogin.isSuccessful) {
                    val usuario = responseLogin.body()!!
                    
                    // Intentamos obtener el negocio asociado
                    var negocioId: Long = -1L
                    try {
                        val responseNegocio = api.obtenerNegocioPorUsuario(usuario.id!!)
                        val negocios = responseNegocio.body()
                        if (responseNegocio.isSuccessful && !negocios.isNullOrEmpty()) {
                            val negocio = negocios[0]
                            negocioId = negocio.id!!
                            sessionManager.saveBaseCaja(negocio.baseCaja)
                        } else {
                            // Búsqueda manual si falla el endpoint directo
                            val responseAll = api.obtenerNegocios()
                            if (responseAll.isSuccessful) {
                                val miNegocio = responseAll.body()?.find { it.usuario.id == usuario.id }
                                if (miNegocio != null) {
                                    negocioId = miNegocio.id!!
                                    sessionManager.saveBaseCaja(miNegocio.baseCaja)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        // Fallback adicional en caso de error de red en el primer intento
                        val responseAll = try { api.obtenerNegocios() } catch (ex: Exception) { null }
                        if (responseAll != null && responseAll.isSuccessful) {
                            val miNegocio = responseAll.body()?.find { it.usuario.id == usuario.id }
                            negocioId = miNegocio?.id ?: -1L
                            miNegocio?.let { sessionManager.saveBaseCaja(it.baseCaja) }
                        }
                    }
                    
                    sessionManager.saveSession(usuario.id!!, negocioId)
                    _uiState.value = LoginUiState.Success(negocioId)
                } else {
                    if (responseLogin.code() == 401) {
                        _uiState.value = LoginUiState.Error("Correo o contraseña incorrectos")
                    } else {
                        _uiState.value = LoginUiState.Error("Error del servidor: ${responseLogin.code()}")
                    }
                }
            } catch (e: Exception) {
                _uiState.value = LoginUiState.Error("Error de conexión: ${e.localizedMessage}")
            }
        }
    }
}

sealed class LoginUiState {
    object Idle : LoginUiState()
    object Loading : LoginUiState()
    data class Success(val negocioId: Long) : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}
