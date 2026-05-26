package com.example.surtiapp.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.surtiapp.data.model.Contacto
import com.example.surtiapp.ui.viewmodel.ContactoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactoScreen(viewModel: ContactoViewModel) {
    val contactos by viewModel.contactos.collectAsState()
    val isLoading by viewModel.loading.collectAsState()
    
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Clientes", "Proveedores")
    
    var showDialog by remember { mutableStateOf(false) }

    val filteredContactos = remember(contactos, selectedTab) {
        val tipo = if (selectedTab == 0) "CLIENTE" else "PROVEEDOR"
        contactos.filter { it.tipo == tipo }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(title = { Text("Agenda de Contactos") })
                TabRow(selectedTabIndex = selectedTab) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title) }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.PersonAdd, contentDescription = "Nuevo Contacto")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            if (isLoading && contactos.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredContactos) { contacto ->
                        ContactoItem(contacto)
                    }
                }
            }

            if (showDialog) {
                AddContactoDialog(
                    onDismiss = { showDialog = false },
                    onConfirm = { nombre, tel, tipo ->
                        viewModel.agregarContacto(nombre, tel, tipo)
                        showDialog = false
                    }
                )
            }
        }
    }
}

@Composable
fun ContactoItem(contacto: Contacto) {
    val context = LocalContext.current
    val colorPrimary = if (contacto.tipo == "CLIENTE") Color(0xFF1976D2) else Color(0xFF7B1FA2)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = androidx.compose.foundation.shape.CircleShape,
                color = colorPrimary.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        if (contacto.tipo == "CLIENTE") Icons.Default.Person else Icons.Default.Business,
                        contentDescription = null,
                        tint = colorPrimary
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(contacto.nombre, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(contacto.telefono, color = Color.Gray, fontSize = 14.sp)
            }
            
            IconButton(onClick = {
                val intent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:${contacto.telefono}")
                }
                context.startActivity(intent)
            }) {
                Icon(Icons.Default.Phone, contentDescription = "Llamar", tint = Color(0xFF2E7D32))
            }
        }
    }
}

@Composable
fun AddContactoDialog(onDismiss: () -> Unit, onConfirm: (String, String, String) -> Unit) {
    var nombre by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var tipo by remember { mutableStateOf("CLIENTE") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo Contacto") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = nombre, onValueChange = { nombre = it }, label = { Text("Nombre Completo") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = telefono, onValueChange = { telefono = it }, label = { Text("Teléfono") }, modifier = Modifier.fillMaxWidth())
                
                Text("Tipo de Contacto:", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = tipo == "CLIENTE", onClick = { tipo = "CLIENTE" })
                    Text("Cliente")
                    Spacer(modifier = Modifier.width(16.dp))
                    RadioButton(selected = tipo == "PROVEEDOR", onClick = { tipo = "PROVEEDOR" })
                    Text("Proveedor")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(nombre, telefono, tipo) },
                enabled = nombre.isNotBlank() && telefono.isNotBlank()
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
