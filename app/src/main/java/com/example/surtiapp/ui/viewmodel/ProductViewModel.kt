package com.example.surtiapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.surtiapp.data.model.Negocio
import com.example.surtiapp.data.model.Producto
import com.example.surtiapp.data.model.Usuario
import com.example.surtiapp.data.network.ApiService
import com.example.surtiapp.data.session.SessionManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ProductViewModel(
    private val api: ApiService,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _allProducts = MutableStateFlow<List<Producto>>(emptyList())
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    // Lista filtrada reactivamente por el SearchBar
    val productos = combine(_allProducts, _searchQuery) { products, query ->
        if (query.isEmpty()) products
        else products.filter { it.nombre.contains(query, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun cargarProductos() {
        viewModelScope.launch {
            val negocioId = sessionManager.negocioId.first() ?: return@launch
            _loading.value = true
            _error.value = null
            try {
                val response = api.getProductos(negocioId)
                if (response.isSuccessful) {
                    _allProducts.value = response.body() ?: emptyList()
                } else {
                    _error.value = "Error: ${response.code()}"
                }
            } catch (e: Exception) {
                _error.value = "Error al cargar inventario: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    fun agregarProducto(
        nombre: String,
        descripcion: String,
        costo: Double,
        venta: Double,
        stock: Int,
        stockMin: Int
    ) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val negocioId = sessionManager.negocioId.first() ?: return@launch
                val nuevo = Producto(
                    nombre = nombre,
                    descripcion = descripcion,
                    precioCosto = costo,
                    precioVenta = venta,
                    stockActual = stock,
                    stockMinimo = stockMin,
                    negocio = Negocio(id = negocioId, nombre = "", tipoNegocio = "", identificacionFiscal = "", usuario = Usuario(nombre = "", email = ""))
                )
                api.crearProducto(nuevo)
                cargarProductos() // Recargamos la lista
            } catch (e: Exception) {
                _error.value = "Error al guardar producto: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }
}
