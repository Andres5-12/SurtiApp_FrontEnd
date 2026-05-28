package com.example.surtiapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.surtiapp.data.model.Categoria
import com.example.surtiapp.data.model.Negocio
import com.example.surtiapp.data.model.Producto
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

    private val _productos = MutableStateFlow<List<Producto>>(emptyList())
    val productos = _productos.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _baseInicial = MutableStateFlow(0.0)
    val baseInicial = _baseInicial.asStateFlow()

    private val _cierres = MutableStateFlow<List<com.example.surtiapp.data.model.CierreCaja>>(emptyList())
    val cierres = _cierres.asStateFlow()

    fun updateBaseInicial(monto: Double) {
        _baseInicial.value = monto
    }

    fun guardarBaseInicial() {
        viewModelScope.launch {
            _loading.value = true
            try {
                val negocioId = sessionManager.negocioId.first() ?: return@launch
                val monto = _baseInicial.value
                
                // Guardamos localmente primero para asegurar persistencia inmediata
                sessionManager.saveBaseCaja(monto)
                
                // Intentamos guardar en el servidor
                val response = api.actualizarBaseCaja(negocioId, monto)
                if (response.isSuccessful) {
                    _error.value = "Base guardada con éxito"
                } else {
                    // Si falla el servidor, al menos lo tenemos local
                    _error.value = "Guardado localmente (Error servidor: ${response.code()})"
                }
            } catch (e: Exception) {
                _error.value = "Guardado localmente (Sin conexión)"
            } finally {
                _loading.value = false
            }
        }
    }

    fun cargarDatos() {
        viewModelScope.launch {
            _loading.value = true
            try {
                val negocioId = sessionManager.negocioId.first() ?: return@launch
                val userId = sessionManager.userId.first() ?: return@launch

                // Cargamos base local como prioridad inicial rápida
                val baseLocal = sessionManager.baseCaja.first()
                if (baseLocal > 0) {
                    _baseInicial.value = baseLocal
                }

                // Intentamos obtener información actualizada del negocio desde el servidor
                val responseNegocio = api.obtenerNegocioPorUsuario(userId)
                if (responseNegocio.isSuccessful) {
                    val listaNegocios = responseNegocio.body()
                    if (!listaNegocios.isNullOrEmpty()) {
                        val baseServidor = listaNegocios[0].baseCaja
                        // Si el servidor tiene un valor diferente y no es 0, actualizamos
                        if (baseServidor > 0 && baseServidor != baseLocal) {
                            _baseInicial.value = baseServidor
                            sessionManager.saveBaseCaja(baseServidor)
                        }
                    }
                }
                
                // Obtenemos cierres
                val responseCierres = api.getCierres(negocioId)
                if (responseCierres.isSuccessful) {
                    _cierres.value = responseCierres.body() ?: emptyList()
                }

                // Obtenemos transacciones activas y pendientes
                val responseActivas = api.getTransacciones(negocioId, "ACTIVO")
                val responsePendientes = api.getTransacciones(negocioId, "PENDIENTE")
                
                val responseC = api.getCategorias()
                val responseP = api.getProductos(negocioId)
                
                if (responseActivas.isSuccessful && responsePendientes.isSuccessful && responseC.isSuccessful) {
                    val activas = responseActivas.body() ?: emptyList()
                    val pendientes = responsePendientes.body() ?: emptyList()
                    
                    _transacciones.value = (activas + pendientes).sortedByDescending { it.fecha }
                    _categorias.value = responseC.body() ?: emptyList()

                    if (responseP.isSuccessful) {
                        _productos.value = responseP.body() ?: emptyList()
                    }
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
        esFiado: Boolean,
        fecha: LocalDateTime = LocalDateTime.now(),
        productoId: Long? = null,
        cantidad: Int = 0,
        metodoPago: String = "EFECTIVO",
        id: Long? = null
    ) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val negocioId = sessionManager.negocioId.first() ?: return@launch
                val userId = sessionManager.userId.first() ?: return@launch
                val categoria = _categorias.value.find { it.id == categoriaId } ?: return@launch
                
                // Calculamos el costo de la mercancía vendida para el reporte de ganancias
                var costoVenta = 0.0
                if (tipo == "INGRESO" && productoId != null) {
                    val prod = _productos.value.find { it.id == productoId }
                    costoVenta = (prod?.precioCosto ?: 0.0) * cantidad
                }

                val nuevaTx = Transaccion(
                    id = id,
                    descripcion = descripcion,
                    monto = monto,
                    costoTotal = costoVenta,
                    tipo = tipo,
                    metodoPago = metodoPago,
                    estado = if (esFiado) "PENDIENTE" else "ACTIVO",
                    fecha = fecha.format(DateTimeFormatter.ISO_DATE_TIME),
                    negocio = Negocio(
                        id = negocioId, 
                        nombre = "", 
                        usuario = Usuario(id = userId, nombre = "", email = "")
                    ),
                    categoria = categoria
                )
                
                val response = if (id == null) {
                    // Verificación de stock para nuevas ventas
                    if (tipo == "INGRESO" && productoId != null) {
                        val producto = _productos.value.find { it.id == productoId }
                        if (producto != null && producto.stockActual < cantidad) {
                            _error.value = "Stock insuficiente para ${producto.nombre}"
                            return@launch
                        }
                    }
                    api.registrarTransaccion(nuevaTx)
                } else {
                    api.actualizarTransaccion(id, nuevaTx)
                }

                if (response.isSuccessful) {
                    // Actualizar el stock del producto si aplica
                    if (productoId != null && cantidad > 0) {
                        val producto = _productos.value.find { it.id == productoId }
                        if (producto != null) {
                            val nuevoStock = if (tipo == "INGRESO") {
                                producto.stockActual - cantidad
                            } else {
                                // Si es un EGRESO, tal vez estamos comprando stock (opcional)
                                producto.stockActual + cantidad 
                            }
                            
                            val productoActualizado = producto.copy(stockActual = nuevoStock)
                            api.crearProducto(productoActualizado)
                        }
                    }
                    cargarDatos()
                } else {
                    _error.value = "Error al guardar: ${response.code()}"
                }
            } catch (e: Exception) {
                _error.value = "Error al procesar: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    fun eliminarMovimiento(id: Long) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val response = api.eliminarTransaccion(id)
                if (response.isSuccessful) {
                    cargarDatos()
                } else {
                    _error.value = "Error al eliminar: ${response.code()}"
                }
            } catch (e: Exception) {
                _error.value = "Error de conexión: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    fun realizarCierre(
        ingEfectivo: Double,
        ingTransferencia: Double,
        egresos: Double,
        saldoInicial: Double,
        saldoReal: Double,
        obs: String,
        utilidad: Double = 0.0
    ) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val negocioId = sessionManager.negocioId.first() ?: return@launch
                val userId = sessionManager.userId.first() ?: return@launch
                val esperado = saldoInicial + ingEfectivo - egresos
                
                val cierre = com.example.surtiapp.data.model.CierreCaja(
                    negocio = Negocio(
                        id = negocioId, 
                        nombre = "", 
                        usuario = Usuario(id = userId, nombre = "", email = "")
                    ),
                    fecha = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")),
                    ingresosEfectivo = ingEfectivo,
                    ingresosTransferencia = ingTransferencia,
                    egresos = egresos,
                    utilidad = utilidad,
                    saldoInicial = saldoInicial,
                    saldoFinalEsperado = esperado,
                    saldoFinalReal = saldoReal,
                    observaciones = obs
                )
                
                val response = api.crearCierre(cierre)
                if (response.isSuccessful) {
                    _error.value = "Cierre realizado con éxito"
                } else {
                    _error.value = "Error al cerrar caja: ${response.code()}"
                }
            } catch (e: Exception) {
                _error.value = "Error: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }
}
