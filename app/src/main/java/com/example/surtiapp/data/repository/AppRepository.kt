package com.example.surtiapp.data.repository

import com.example.surtiapp.data.model.*
import com.example.surtiapp.data.network.ApiService
import retrofit2.Response

class AppRepository(private val api: ApiService) {

    suspend fun getProductos(negocioId: Long): Result<List<Producto>> {
        return try {
            val response = api.getProductos(negocioId)
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception("Error al obtener productos: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCategorias(): Result<List<Categoria>> {
        return try {
            val response = api.getCategorias()
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception("Error al obtener categorías: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTransacciones(negocioId: Long): Result<List<Transaccion>> {
        return try {
            val response = api.getTransacciones(negocioId)
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception("Error al obtener transacciones: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
