package com.example.surtiapp.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.People
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Pantalla(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Perfil : Pantalla("perfil", "Perfil", Icons.Default.Business)
    object Inventario : Pantalla("inventario", "Inventario", Icons.Default.Inventory)
    object Transacciones : Pantalla("transacciones", "Finanzas", Icons.Default.Paid)
    object Contactos : Pantalla("contactos", "Contactos", Icons.Default.People)
    
    // Rutas sin bottom bar
    object Login : Pantalla("login", "Login", Icons.Default.Business)
    object Registro : Pantalla("registro", "Registro", Icons.Default.Business)
}

val itemsNavegacion = listOf(
    Pantalla.Inventario,
    Pantalla.Transacciones,
    Pantalla.Contactos,
    Pantalla.Perfil
)
