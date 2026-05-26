package com.example.surtiapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.surtiapp.data.model.Contacto
import com.example.surtiapp.data.model.Negocio
import com.example.surtiapp.data.model.Usuario
import com.example.surtiapp.data.network.ApiService
import com.example.surtiapp.data.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ContactoViewModel(
    private val api: ApiService,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _contactos = MutableStateFlow<List<Contacto>>(emptyList())
    val contactos = _contactos.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading = _loading.asStateFlow()

    fun cargarContactos() {
        viewModelScope.launch {
            val negocioId = sessionManager.negocioId.first() ?: return@launch
            _loading.value = true
            try {
                _contactos.value = api.getContactos(negocioId)
            } catch (e: Exception) {
                // Manejar error
            } finally {
                _loading.value = false
            }
        }
    }

    fun agregarContacto(nombre: String, telefono: String, tipo: String) {
        viewModelScope.launch {
            val negocioId = sessionManager.negocioId.first() ?: return@launch
            _loading.value = true
            try {
                val nuevo = Contacto(
                    nombre = nombre,
                    telefono = telefono,
                    tipo = tipo,
                    negocio = Negocio(id = negocioId, nombre = "", tipoNegocio = "", identificacionFiscal = "", usuario = Usuario(nombre = "", email = ""))
                )
                api.crearContacto(nuevo)
                cargarContactos()
            } catch (e: Exception) {
                // Manejar error
            } finally {
                _loading.value = false
            }
        }
    }
}
