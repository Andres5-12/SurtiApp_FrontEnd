package com.example.surtiapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.surtiapp.data.model.Usuario
import com.example.surtiapp.data.network.ApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RegistroViewModel(private val api: ApiService) : ViewModel() {
    private val _uiState = MutableStateFlow<RegistroUiState>(RegistroUiState.Idle)
    val uiState = _uiState.asStateFlow()

    fun registrar(nombreU: String, email: String, pass: String) {
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$".toRegex()
        if (!email.matches(emailRegex)) {
            _uiState.value = RegistroUiState.Error("Correo electrónico no válido (ej: usuario@dominio.com)")
            return
        }

        viewModelScope.launch {
            _uiState.value = RegistroUiState.Loading
            try {
                // Registro de Usuario solamente
                val response = api.registrarUsuario(Usuario(nombre = nombreU, email = email, password = pass))
                if (response.isSuccessful) {
                    val user = response.body()!!
                    _uiState.value = RegistroUiState.Success(user.id ?: 0)
                } else {
                    val errorBody = response.errorBody()?.string() ?: ""
                    val errorMessage = when {
                        response.code() == 409 || errorBody.contains("exists", ignoreCase = true) || errorBody.contains("ya existe", ignoreCase = true) -> 
                            "Error en el registro: ya existe una cuenta registrada con este correo."
                        response.code() == 400 -> "Datos de registro inválidos. Verifica el correo y la contraseña."
                        else -> "Error en el registro (${response.code()}): $errorBody"
                    }
                    _uiState.value = RegistroUiState.Error(errorMessage)
                }
            } catch (e: Exception) {
                _uiState.value = RegistroUiState.Error(e.message ?: "Error desconocido")
            }
        }
    }
}

sealed class RegistroUiState {
    object Idle : RegistroUiState()
    object Loading : RegistroUiState()
    data class Success(val userId: Long) : RegistroUiState()
    data class Error(val message: String) : RegistroUiState()
}
