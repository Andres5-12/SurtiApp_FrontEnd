package com.example.surtiapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.surtiapp.data.model.Negocio
import com.example.surtiapp.ui.viewmodel.NegocioUiState
import com.example.surtiapp.ui.viewmodel.NegocioViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NegocioProfileScreen(viewModel: NegocioViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    var showEditDialog by remember { mutableStateOf(false) }
    var showLogoutConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { 
            TopAppBar(
                title = { Text("Mi Perfil") },
                actions = {
                    IconButton(onClick = { showLogoutConfirm = true }) {
                        Icon(Icons.Default.Logout, contentDescription = "Cerrar Sesión", tint = MaterialTheme.colorScheme.error)
                    }
                }
            ) 
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            when (val state = uiState) {
                is NegocioUiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is NegocioUiState.Success -> {
                    UserInfoSection(state.negocio.usuario)
                    Spacer(modifier = Modifier.height(16.dp))
                    NegocioInfoCard(
                        negocio = state.negocio,
                        onEdit = { showEditDialog = true }
                    )
                    
                    if (showEditDialog) {
                        EditNegocioDialog(
                            negocio = state.negocio,
                            onDismiss = { showEditDialog = false },
                            onConfirm = { nombre, tipo, nit ->
                                viewModel.actualizarNegocio(state.negocio.id!!, nombre, tipo, nit, state.negocio.usuario.id!!)
                                showEditDialog = false
                            }
                        )
                    }
                }
                is NegocioUiState.NoNegocio -> {
                    RegistroNegocioForm(
                        userId = state.userId,
                        onRegistrar = { nombre, tipo, nit ->
                            viewModel.registrarNegocio(nombre, tipo, nit, state.userId)
                        }
                    )
                }
                is NegocioUiState.Error -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = state.message, color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            if (showLogoutConfirm) {
                AlertDialog(
                    onDismissRequest = { showLogoutConfirm = false },
                    title = { Text("Cerrar Sesión") },
                    text = { Text("¿Estás seguro de que deseas cerrar tu sesión actual?") },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.logout()
                                showLogoutConfirm = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Cerrar Sesión")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showLogoutConfirm = false }) {
                            Text("Cancelar")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun UserInfoSection(usuario: com.example.surtiapp.data.model.Usuario) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(56.dp),
                shape = androidx.compose.foundation.shape.CircleShape,
                color = MaterialTheme.colorScheme.primary
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = usuario.nombre.take(1).uppercase(),
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(usuario.nombre, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text(usuario.email, color = Color.Gray, fontSize = 14.sp)
                Surface(
                    modifier = Modifier.padding(top = 4.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Text(
                        "ADMINISTRADOR",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun NegocioInfoCard(negocio: Negocio, onEdit: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Store,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Datos de la Empresa", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                InfoRow(Icons.Default.Business, "Nombre del Negocio", negocio.nombre)
                InfoRow(Icons.Default.Category, "Tipo de Negocio", negocio.tipoNegocio ?: "No especificado")
                InfoRow(Icons.Default.Badge, "Identificación Fiscal (NIT)", negocio.identificacionFiscal ?: "No especificado")
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = onEdit,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Editar Datos del Negocio")
                }
            }
        }
    }
}

@Composable
fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Text(value, fontWeight = FontWeight.Medium, fontSize = 15.sp)
        }
    }
}

@Composable
fun EditNegocioDialog(negocio: Negocio, onDismiss: () -> Unit, onConfirm: (String, String, String) -> Unit) {
    var nombre by remember { mutableStateOf(negocio.nombre) }
    var tipo by remember { mutableStateOf(negocio.tipoNegocio ?: "") }
    var nit by remember { mutableStateOf(negocio.identificacionFiscal ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Negocio") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = nombre, onValueChange = { nombre = it }, label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = tipo, onValueChange = { tipo = it }, label = { Text("Tipo") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = nit, onValueChange = { nit = it }, label = { Text("NIT / ID") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(nombre, tipo, nit) }, enabled = nombre.isNotBlank()) {
                Text("Guardar Cambios")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
fun RegistroNegocioForm(userId: Long, onRegistrar: (String, String, String) -> Unit) {
    var nombre by remember { mutableStateOf("") }
    var tipo by remember { mutableStateOf("") }
    var nit by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Registra tu Empresa",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            "Cuéntanos sobre tu negocio para empezar",
            fontSize = 14.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        OutlinedTextField(
            value = nombre,
            onValueChange = { nombre = it },
            label = { Text("Nombre del Negocio") },
            leadingIcon = { Icon(Icons.Default.Storefront, contentDescription = null) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = tipo,
            onValueChange = { tipo = it },
            label = { Text("Tipo de Negocio (Tienda, Fruver, etc.)") },
            leadingIcon = { Icon(Icons.Default.Category, contentDescription = null) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = nit,
            onValueChange = { nit = it },
            label = { Text("NIT / Cédula") },
            leadingIcon = { Icon(Icons.Default.AssignmentInd, contentDescription = null) },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = { onRegistrar(nombre, tipo, nit) },
            modifier = Modifier.fillMaxWidth(),
            enabled = nombre.isNotBlank() && tipo.isNotBlank() && nit.isNotBlank()
        ) {
            Text("Registrar Empresa", modifier = Modifier.padding(8.dp))
        }
    }
}
