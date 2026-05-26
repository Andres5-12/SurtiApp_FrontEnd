package com.example.surtiapp.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.surtiapp.data.model.Producto
import com.example.surtiapp.ui.viewmodel.ProductViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductInventoryScreen(viewModel: ProductViewModel) {
    val productos by viewModel.productos.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isLoading by viewModel.loading.collectAsState()
    
    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    Scaffold(
        topBar = {
            Column {
                TopAppBar(title = { Text("Inventario y Productos") })
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { viewModel.updateSearchQuery(it) },
                    onSearch = {},
                    active = false,
                    onActiveChange = {},
                    placeholder = { Text("Buscar producto...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {}
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showBottomSheet = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Añadir Producto")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            if (isLoading && productos.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(productos) { producto ->
                        ProductItem(producto)
                    }
                }
            }

            if (showBottomSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showBottomSheet = false },
                    sheetState = sheetState
                ) {
                    AddProductForm(
                        onDismiss = { showBottomSheet = false },
                        onConfirm = { n, d, pc, pv, s, sm ->
                            viewModel.agregarProducto(n, d, pc, pv, s, sm)
                            showBottomSheet = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ProductItem(producto: Producto) {
    val esBajoStock = producto.stockActual <= producto.stockMinimo

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (esBajoStock) Color(0xFFFFEBEE) else MaterialTheme.colorScheme.surface
        ),
        border = if (esBajoStock) BorderStroke(2.dp, Color.Red) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(producto.nombre, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    if (esBajoStock) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = Color.Red,
                            shape = MaterialTheme.shapes.extraSmall
                        ) {
                            Text(
                                "¡SURTIR!",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    }
                }
                Text(producto.descripcion, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row {
                    Column(Modifier.weight(1f)) {
                        Text("Venta", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text("$${producto.precioVenta}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Costo", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text("$${producto.precioCosto}", color = Color.Gray)
                    }
                }
            }
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Stock", style = MaterialTheme.typography.labelSmall)
                Text(
                    text = producto.stockActual.toString(),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = if (esBajoStock) Color.Red else Color.Unspecified
                )
            }
        }
    }
}

@Composable
fun AddProductForm(onDismiss: () -> Unit, onConfirm: (String, String, Double, Double, Int, Int) -> Unit) {
    var nombre by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var precioCosto by remember { mutableStateOf("") }
    var precioVenta by remember { mutableStateOf("") }
    var stockActual by remember { mutableStateOf("") }
    var stockMinimo by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
            .verticalScroll(androidx.compose.foundation.rememberScrollState())
    ) {
        Text("Nuevo Producto", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(value = nombre, onValueChange = { nombre = it }, label = { Text("Nombre del Producto") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Descripción") }, modifier = Modifier.fillMaxWidth())
        
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = precioCosto, onValueChange = { precioCosto = it }, 
                label = { Text("Precio Costo") }, modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
            OutlinedTextField(
                value = precioVenta, onValueChange = { precioVenta = it }, 
                label = { Text("Precio Venta") }, modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
        }
        
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = stockActual, onValueChange = { stockActual = it }, 
                label = { Text("Stock Inicial") }, modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            OutlinedTextField(
                value = stockMinimo, onValueChange = { stockMinimo = it }, 
                label = { Text("Stock Mínimo") }, modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = { 
                onConfirm(
                    nombre, desc, 
                    precioCosto.toDoubleOrNull() ?: 0.0, 
                    precioVenta.toDoubleOrNull() ?: 0.0, 
                    stockActual.toIntOrNull() ?: 0, 
                    stockMinimo.toIntOrNull() ?: 0
                ) 
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = nombre.isNotBlank() && precioVenta.isNotBlank()
        ) {
            Text("Guardar Producto")
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}
