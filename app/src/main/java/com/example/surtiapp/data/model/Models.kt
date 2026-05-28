package com.example.surtiapp.data.model

import com.google.gson.annotations.SerializedName

data class Usuario(
    val id: Long? = null,
    val nombre: String,
    val email: String,
    val password: String? = null,
    @SerializedName("fechaCreacion") val fechaCreacion: String? = null
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class Negocio(
    val id: Long? = null,
    val nombre: String,
    @SerializedName("tipoNegocio") val tipoNegocio: String? = null,
    @SerializedName("identificacionFiscal") val identificacionFiscal: String? = null,
    @SerializedName("baseCaja") val baseCaja: Double = 0.0,
    val usuario: Usuario
)

data class Categoria(
    val id: Long? = null,
    val nombre: String,
    val descripcion: String? = null,
    val tipo: String // "INGRESO" o "EGRESO"
)

data class Producto(
    val id: Long? = null,
    val nombre: String,
    val descripcion: String? = null,
    @SerializedName("precioVenta") val precioVenta: Double,
    @SerializedName("precioCosto") val precioCosto: Double,
    @SerializedName("stockActual") val stockActual: Int = 0,
    @SerializedName("stockMinimo") val stockMinimo: Int = 5,
    val negocio: Negocio
)

data class Contacto(
    val id: Long? = null,
    val nombre: String,
    val telefono: String? = null,
    val tipo: String, // "CLIENTE" o "PROVEEDOR"
    val negocio: Negocio
)

data class Transaccion(
    val id: Long? = null,
    val negocio: Negocio,
    val categoria: Categoria,
    val descripcion: String,
    val monto: Double,
    @SerializedName("costoTotal") val costoTotal: Double? = 0.0, // COGS (Costo de lo vendido)
    val tipo: String, // "INGRESO" o "EGRESO"
    @SerializedName("metodoPago") val metodoPago: String? = "EFECTIVO", // "EFECTIVO", "NEQUI", "DAVIPLATA", "DATAFONO"
    val estado: String? = "ACTIVO", // "ACTIVO", "ANULADO", "PENDIENTE"
    val fecha: String? = null,
    @SerializedName("fechaRegistro") val fechaRegistro: String? = null
)

data class CierreCaja(
    val id: Long? = null,
    val negocio: Negocio,
    val fecha: String,
    @SerializedName("ingresosEfectivo") val ingresosEfectivo: Double,
    @SerializedName("ingresosTransferencia") val ingresosTransferencia: Double,
    val egresos: Double,
    @SerializedName("utilidad") val utilidad: Double? = 0.0, // Ganancia real (Ingresos - Costos - Gastos)
    @SerializedName("saldoInicial") val saldoInicial: Double,
    @SerializedName("saldoFinalEsperado") val saldoFinalEsperado: Double,
    @SerializedName("saldoFinalReal") val saldoFinalReal: Double,
    val observaciones: String? = null
)
