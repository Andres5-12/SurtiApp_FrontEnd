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

    private val _allAvailableProducts = MutableStateFlow<List<Producto>>(emptyList())
    val allAvailableProducts = _allAvailableProducts.asStateFlow()

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
                // Obtenemos solo los productos filtrados por negocioId directamente desde el API
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

    fun agregarProductoExistente(producto: Producto, cantidad: Int) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                val negocioId = sessionManager.negocioId.first() ?: return@launch
                val userId = sessionManager.userId.first() ?: return@launch
                
                val nuevo = producto.copy(
                    id = null,
                    stockActual = cantidad,
                    negocio = Negocio(
                        id = negocioId, 
                        nombre = "", 
                        usuario = Usuario(id = userId, nombre = "", email = "")
                    )
                )
                val response = api.crearProducto(nuevo)
                if (response.isSuccessful) {
                    cargarProductos()
                } else {
                    _error.value = "Error al guardar: ${response.code()}"
                }
            } catch (e: Exception) {
                _error.value = "Error al agregar producto existente: ${e.message}"
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
            _error.value = null
            try {
                val negocioId = sessionManager.negocioId.first() ?: return@launch
                val userId = sessionManager.userId.first() ?: return@launch
                
                val nuevo = Producto(
                    nombre = nombre,
                    descripcion = descripcion,
                    precioCosto = costo,
                    precioVenta = venta,
                    stockActual = stock,
                    stockMinimo = stockMin,
                    negocio = Negocio(
                        id = negocioId, 
                        nombre = "", 
                        usuario = Usuario(id = userId, nombre = "", email = "")
                    )
                )
                val response = api.crearProducto(nuevo)
                if (response.isSuccessful) {
                    cargarProductos() // Recargamos la lista
                } else {
                    _error.value = "Error al servidor (${response.code()}): ${response.errorBody()?.string()}"
                }
            } catch (e: Exception) {
                _error.value = "Error al guardar producto: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    fun actualizarProducto(
        id: Long,
        nombre: String,
        descripcion: String,
        costo: Double,
        venta: Double,
        stock: Int,
        stockMin: Int
    ) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                val negocioId = sessionManager.negocioId.first() ?: return@launch
                val userId = sessionManager.userId.first() ?: return@launch
                
                val editado = Producto(
                    id = id,
                    nombre = nombre,
                    descripcion = descripcion,
                    precioCosto = costo,
                    precioVenta = venta,
                    stockActual = stock,
                    stockMinimo = stockMin,
                    negocio = Negocio(
                        id = negocioId, 
                        nombre = "", 
                        usuario = Usuario(id = userId, nombre = "", email = "")
                    )
                )
                val response = api.crearProducto(editado)
                if (response.isSuccessful) {
                    cargarProductos()
                } else {
                    _error.value = "Error al actualizar (${response.code()})"
                }
            } catch (e: Exception) {
                _error.value = "Error al actualizar: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    fun eliminarProducto(id: Long) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val response = api.eliminarProducto(id)
                if (response.isSuccessful) {
                    cargarProductos()
                } else {
                    _error.value = "Error al eliminar producto (${response.code()})"
                }
            } catch (e: Exception) {
                _error.value = "Error de conexión: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }
}
