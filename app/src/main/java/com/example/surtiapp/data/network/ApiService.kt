package com.example.surtiapp.data.network

import com.example.surtiapp.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    @POST("/api/usuarios")
    suspend fun registrarUsuario(@Body usuario: Usuario): Usuario

    @POST("/api/usuarios/login")
    suspend fun login(@Body request: LoginRequest): Usuario

    @POST("/api/negocios")
    suspend fun crearNegocio(@Body negocio: Negocio): Negocio

    @GET("/api/negocios/usuario/{usuarioId}")
    suspend fun obtenerNegocioPorUsuario(@Path("usuarioId") usuarioId: Long): Negocio

    @GET("/api/categorias")
    suspend fun getCategorias(): Response<List<Categoria>>

    @GET("/api/productos")
    suspend fun getProductos(@Query("negocioId") negocioId: Long): Response<List<Producto>>

    @POST("/api/productos")
    suspend fun crearProducto(@Body producto: Producto): Producto

    @GET("/api/contactos")
    suspend fun getContactos(@Query("negocioId") negocioId: Long): List<Contacto>

    @POST("/api/contactos")
    suspend fun crearContacto(@Body contacto: Contacto): Contacto

    @GET("/api/transacciones")
    suspend fun getTransacciones(@Query("negocioId") negocioId: Long): Response<List<Transaccion>>

    @POST("/api/transacciones")
    suspend fun registrarTransaccion(@Body transaccion: Transaccion): Transaccion

    @GET("/api/transacciones/pendientes")
    suspend fun getTransaccionesPendientes(@Query("negocioId") negocioId: Long): List<Transaccion>
}
