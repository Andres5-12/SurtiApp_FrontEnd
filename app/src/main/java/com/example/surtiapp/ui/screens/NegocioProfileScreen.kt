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
import com.example.surtiapp.data.model.Negocio
import com.example.surtiapp.ui.viewmodel.NegocioUiState
import com.example.surtiapp.ui.viewmodel.NegocioViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NegocioProfileScreen(viewModel: NegocioViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Mi Negocio") }) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = uiState) {
                is NegocioUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is NegocioUiState.Success -> {
                    NegocioInfoCard(state.negocio)
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
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}

@Composable
fun NegocioInfoCard(negocio: Negocio) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
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
                Icon(
                    Icons.Default.Store,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = negocio.nombre,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = negocio.tipoNegocio,
                    color = Color.Gray,
                    fontSize = 16.sp
                )
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                
                Row(modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Badge, contentDescription = null, tint = Color.Gray)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Identificación Fiscal", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text(negocio.identificacionFiscal, fontWeight = FontWeight.Medium)
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = { /* Acción para editar */ },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Modificar Datos")
                }
            }
        }
    }
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
