package com.example.surtiapp.data.model

data class Usuario(
    val id: Long? = null,
    val nombre: String,
    val email: String,
    val password: String? = null
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class Negocio(
    val id: Long? = null,
    val nombre: String,
    val tipoNegocio: String,
    val identificacionFiscal: String,
    val usuario: Usuario
)

data class Categoria(
    val id: Long,
    val nombre: String,
    val descripcion: String,
    val tipo: String // "INGRESO" o "EGRESO"
)

data class Producto(
    val id: Long? = null,
    val nombre: String,
    val descripcion: String,
    val precioCosto: Double,
    val precioVenta: Double,
    val stockActual: Int,
    val stockMinimo: Int,
    val negocio: Negocio
)

data class Contacto(
    val id: Long? = null,
    val nombre: String,
    val telefono: String,
    val tipo: String, // "CLIENTE" o "PROVEEDOR"
    val negocio: Negocio
)

data class Transaccion(
    val id: Long? = null,
    val descripcion: String,
    val monto: Double,
    val tipo: String, // "INGRESO" o "EGRESO"
    val estado: String, // "ACTIVO", "ANULADO", "PENDIENTE"
    val fecha: String,
    val negocio: Negocio,
    val categoria: Categoria
)
