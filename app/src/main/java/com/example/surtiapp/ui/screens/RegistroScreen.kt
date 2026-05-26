package com.example.surtiapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.surtiapp.ui.viewmodel.RegistroViewModel
import com.example.surtiapp.ui.viewmodel.RegistroUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistroScreen(
    viewModel: RegistroViewModel,
    onRegistroSuccess: (Long, Long) -> Unit,
    onBack: () -> Unit
) {
    var nombreU by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var nombreN by remember { mutableStateOf("") }
    var tipoN by remember { mutableStateOf("") }
    var nit by remember { mutableStateOf("") }

    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState) {
        if (uiState is RegistroUiState.Success) {
            val state = uiState as RegistroUiState.Success
            onRegistroSuccess(state.userId, state.negocioId)
        }
    }

    Scaffold(
        topBar = { 
            TopAppBar(
                title = { Text("Crear Nueva Cuenta") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                    }
                }
            ) 
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Información Personal", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = nombreU, 
                onValueChange = { nombreU = it }, 
                label = { Text("Nombre Completo") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = email, 
                onValueChange = { email = it }, 
                label = { Text("Email") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = pass, 
                onValueChange = { pass = it }, 
                label = { Text("Contraseña") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                trailingIcon = {
                    val image = if (passwordVisible)
                        Icons.Default.Visibility
                    else Icons.Default.VisibilityOff

                    val description = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña"

                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(imageVector = image, contentDescription = description)
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))
            Text("Detalles del Negocio", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = nombreN, 
                onValueChange = { nombreN = it }, 
                label = { Text("Nombre del Negocio") },
                leadingIcon = { Icon(Icons.Default.Store, contentDescription = null) },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = tipoN, 
                onValueChange = { tipoN = it }, 
                label = { Text("Tipo de Negocio") },
                leadingIcon = { Icon(Icons.Default.Category, contentDescription = null) },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = nit, 
                onValueChange = { nit = it }, 
                label = { Text("NIT / Identificación") },
                leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(40.dp))

            if (uiState is RegistroUiState.Loading) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = { viewModel.registrar(nombreU, email, pass, nombreN, tipoN, nit) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("Registrarme ahora", modifier = Modifier.padding(8.dp))
                }
                
                if (uiState is RegistroUiState.Error) {
                    Text(
                        (uiState as RegistroUiState.Error).message, 
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            }
        }
    }
}
