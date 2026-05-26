package com.example.surtiapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.surtiapp.data.model.Categoria
import com.example.surtiapp.data.model.Negocio
import com.example.surtiapp.data.model.Transaccion
import com.example.surtiapp.data.model.Usuario
import com.example.surtiapp.data.network.ApiService
import com.example.surtiapp.data.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class TransactionViewModel(
    private val api: ApiService,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _transacciones = MutableStateFlow<List<Transaccion>>(emptyList())
    val transacciones = _transacciones.asStateFlow()

    private val _categorias = MutableStateFlow<List<Categoria>>(emptyList())
    val categorias = _categorias.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    fun cargarDatos() {
        viewModelScope.launch {
            _loading.value = true
            try {
                val negocioId = sessionManager.negocioId.first() ?: return@launch
                val responseT = api.getTransacciones(negocioId)
                val responseC = api.getCategorias()
                
                if (responseT.isSuccessful && responseC.isSuccessful) {
                    _transacciones.value = responseT.body() ?: emptyList()
                    _categorias.value = responseC.body() ?: emptyList()
                } else {
                    _error.value = "Error al cargar datos del servidor"
                }
            } catch (e: Exception) {
                _error.value = "Error al cargar datos: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    fun registrarMovimiento(
        descripcion: String,
        monto: Double,
        tipo: String,
        categoriaId: Long,
        esFiado: Boolean
    ) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val negocioId = sessionManager.negocioId.first() ?: return@launch
                val categoria = _categorias.value.find { it.id == categoriaId } ?: return@launch
                
                val nuevaTx = Transaccion(
                    descripcion = descripcion,
                    monto = monto,
                    tipo = tipo,
                    estado = if (esFiado) "PENDIENTE" else "ACTIVO",
                    fecha = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME),
                    negocio = Negocio(id = negocioId, nombre = "", tipoNegocio = "", identificacionFiscal = "", usuario = Usuario(nombre = "", email = "")),
                    categoria = categoria
                )
                
                api.registrarTransaccion(nuevaTx)
                cargarDatos()
            } catch (e: Exception) {
                _error.value = "Error al registrar: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }
}
