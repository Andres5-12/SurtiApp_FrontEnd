package com.example.surtiapp.data.network

import com.example.surtiapp.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    @POST("api/usuarios")
    suspend fun registrarUsuario(@Body usuario: Usuario): Response<Usuario>

    @POST("api/usuarios/login")
    suspend fun login(@Body request: LoginRequest): Response<Usuario>

    @POST("api/negocios")
    suspend fun crearNegocio(@Body negocio: Negocio): Response<Negocio>

    @GET("api/negocios")
    suspend fun obtenerNegocios(): Response<List<Negocio>>

    @GET("api/negocios/usuario/{usuarioId}")
    suspend fun obtenerNegocioPorUsuario(@Path("usuarioId") usuarioId: Long): Response<List<Negocio>>

    @PATCH("api/negocios/{id}/base")
    suspend fun actualizarBaseCaja(@Path("id") id: Long, @Query("monto") monto: Double): Response<Void>

    @GET("api/categorias")
    suspend fun getCategorias(): Response<List<Categoria>>

    @GET("api/productos")
    suspend fun getProductosBase(): Response<List<Producto>>

    @GET("api/productos")
    suspend fun getProductos(@Query("negocioId") negocioId: Long): Response<List<Producto>>

    @POST("api/productos")
    suspend fun crearProducto(@Body producto: Producto): Response<Producto>

    @GET("api/contactos")
    suspend fun getContactos(@Query("negocioId") negocioId: Long): Response<List<Contacto>>

    @POST("api/contactos")
    suspend fun crearContacto(@Body contacto: Contacto): Response<Contacto>

    @DELETE("api/contactos/{id}")
    suspend fun eliminarContacto(@Path("id") id: Long): Response<Void>

    @GET("api/transacciones/negocio/{negocioId}")
    suspend fun getTransacciones(
        @Path("negocioId") negocioId: Long,
        @Query("estado") estado: String = "ACTIVO"
    ): Response<List<Transaccion>>

    @POST("api/transacciones")
    suspend fun registrarTransaccion(@Body transaccion: Transaccion): Response<Transaccion>

    @PUT("api/transacciones/{id}")
    suspend fun actualizarTransaccion(@Path("id") id: Long, @Body transaccion: Transaccion): Response<Transaccion>

    @DELETE("api/transacciones/{id}")
    suspend fun eliminarTransaccion(@Path("id") id: Long): Response<Void>

    @DELETE("api/productos/{id}")
    suspend fun eliminarProducto(@Path("id") id: Long): Response<Void>

    @GET("api/transacciones/pendientes")
    suspend fun getTransaccionesPendientes(@Query("negocioId") negocioId: Long): List<Transaccion>

    // Cierre de Caja
    @POST("api/cierres")
    suspend fun crearCierre(@Body cierre: CierreCaja): Response<CierreCaja>

    @GET("api/cierres/negocio/{negocioId}")
    suspend fun getCierres(@Path("negocioId") negocioId: Long): Response<List<CierreCaja>>
}
