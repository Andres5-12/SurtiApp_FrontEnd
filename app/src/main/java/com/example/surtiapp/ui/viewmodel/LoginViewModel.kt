package com.example.surtiapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.surtiapp.data.model.LoginRequest
import com.example.surtiapp.data.network.ApiService
import com.example.surtiapp.data.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

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
                val usuario = api.login(request)
                
                // Obtenemos el negocio asociado para guardarlo en la sesión
                val negocio = api.obtenerNegocioPorUsuario(usuario.id!!)
                
                sessionManager.saveSession(usuario.id, negocio.id!!)
                
                _uiState.value = LoginUiState.Success(negocio.id)
            } catch (e: HttpException) {
                if (e.code() == 401) {
                    _uiState.value = LoginUiState.Error("Correo o contraseña incorrectos")
                } else {
                    _uiState.value = LoginUiState.Error("Error del servidor: ${e.code()}")
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
