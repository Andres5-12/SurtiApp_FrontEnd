package com.example.surtiapp.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.surtiapp.data.network.RetrofitClient
import com.example.surtiapp.data.session.SessionManager
import com.example.surtiapp.ui.navigation.Pantalla
import com.example.surtiapp.ui.navigation.itemsNavegacion
import com.example.surtiapp.ui.viewmodel.*

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val api = RetrofitClient.apiService
    
    val factory = viewModelFactory {
        initializer { TransactionViewModel(api, sessionManager) }
        initializer { RegistroViewModel(api) }
        initializer { LoginViewModel(api, sessionManager) }
        initializer { NegocioViewModel(api, sessionManager) }
        initializer { ProductViewModel(api, sessionManager) }
        initializer { ContactoViewModel(api, sessionManager) }
    }

    // Determinar si mostrar la barra inferior (solo en las pantallas principales)
    val showBottomBar = itemsNavegacion.any { it.route == currentDestination?.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    itemsNavegacion.forEach { pantalla ->
                        NavigationBarItem(
                            icon = { Icon(pantalla.icon, contentDescription = pantalla.title) },
                            label = { Text(pantalla.title) },
                            selected = currentDestination?.hierarchy?.any { it.route == pantalla.route } == true,
                            onClick = {
                                navController.navigate(pantalla.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Pantalla.Login.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Pantalla.Login.route) {
                val loginViewModel: LoginViewModel = viewModel(factory = factory)
                LoginScreen(
                    viewModel = loginViewModel,
                    onLoginSuccess = { negocioId ->
                        navController.navigate(Pantalla.Inventario.route) {
                            popUpTo(Pantalla.Login.route) { inclusive = true }
                        }
                    },
                    onNavigateToRegister = {
                        navController.navigate(Pantalla.Registro.route)
                    }
                )
            }
            
            composable(Pantalla.Registro.route) {
                val registroViewModel: RegistroViewModel = viewModel(factory = factory)
                RegistroScreen(
                    viewModel = registroViewModel,
                    onRegistroSuccess = { _, _ ->
                        navController.navigate(Pantalla.Inventario.route) {
                            popUpTo(Pantalla.Login.route) { inclusive = true }
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Pantalla.Inventario.route) {
                val productViewModel: ProductViewModel = viewModel(factory = factory)
                LaunchedEffect(Unit) {
                    productViewModel.cargarProductos()
                }
                ProductInventoryScreen(viewModel = productViewModel)
            }

            composable(Pantalla.Transacciones.route) {
                val txViewModel: TransactionViewModel = viewModel(factory = factory)
                LaunchedEffect(Unit) {
                    txViewModel.cargarDatos()
                }
                TransactionHistoryScreen(viewModel = txViewModel)
            }

            composable(Pantalla.Contactos.route) {
                val contactoViewModel: ContactoViewModel = viewModel(factory = factory)
                LaunchedEffect(Unit) {
                    contactoViewModel.cargarContactos()
                }
                ContactoScreen(viewModel = contactoViewModel)
            }

            composable(Pantalla.Perfil.route) {
                val negocioViewModel: NegocioViewModel = viewModel(factory = factory)
                NegocioProfileScreen(viewModel = negocioViewModel)
            }
        }
    }
}
