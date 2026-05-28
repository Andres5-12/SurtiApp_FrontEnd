package com.example.surtiapp.ui.screens

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
    val error by viewModel.error.collectAsState()
    
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Todos", "Bajo Stock", "Sin Stock")
    
    var showBottomSheet by remember { mutableStateOf(false) }
    var productToEdit by remember { mutableStateOf<Producto?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<Producto?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val snackbarHostState = remember { SnackbarHostState() }

    val filteredProducts = remember(productos, selectedTab, searchQuery) {
        val baseList = when (selectedTab) {
            1 -> productos.filter { it.stockActual > 0 && it.stockActual <= it.stockMinimo }
            2 -> productos.filter { it.stockActual <= 0 }
            else -> productos
        }
        
        if (searchQuery.isBlank()) {
            baseList
        } else {
            baseList.filter { it.nombre.contains(searchQuery, ignoreCase = true) }
        }
    }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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

                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    tabs.forEachIndexed { index, title ->
                        SegmentedButton(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = tabs.size)
                        ) {
                            Text(title, fontSize = 12.sp)
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { 
                    showBottomSheet = true 
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nuevo Producto")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            if (isLoading && productos.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                if (filteredProducts.isEmpty() && !isLoading) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val emptyIcon = when(selectedTab) {
                            1 -> Icons.Default.ThumbUp
                            2 -> Icons.Default.CheckCircle
                            else -> Icons.Default.Inventory2
                        }
                        val emptyText = when(selectedTab) {
                            1 -> "No hay productos con stock bajo"
                            2 -> "¡Felicidades! No tienes productos agotados"
                            else -> "No hay productos en tu inventario"
                        }
                        
                        Icon(emptyIcon, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(emptyText, color = Color.Gray)
                        if (selectedTab == 0) {
                            Text("Usa el botón + para registrar uno", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredProducts) { producto ->
                            ProductItem(
                                producto = producto,
                                onClick = {
                                    productToEdit = producto
                                    showBottomSheet = true
                                }
                            )
                        }
                    }
                }
            }

            if (showBottomSheet) {
                ModalBottomSheet(
                    onDismissRequest = { 
                        showBottomSheet = false
                        productToEdit = null
                    },
                    sheetState = sheetState
                ) {
                    AddProductForm(
                        productToEdit = productToEdit,
                        onDismiss = { 
                            showBottomSheet = false
                            productToEdit = null
                        },
                        onDelete = {
                            showDeleteConfirm = productToEdit
                            showBottomSheet = false
                        },
                        onConfirm = { n, d, pc, pv, s, sm ->
                            if (productToEdit == null) {
                                viewModel.agregarProducto(n, d, pc, pv, s, sm)
                            } else {
                                viewModel.actualizarProducto(productToEdit!!.id!!, n, d, pc, pv, s, sm)
                            }
                            showBottomSheet = false
                            productToEdit = null
                        }
                    )
                }
            }

            if (showDeleteConfirm != null) {
                AlertDialog(
                    onDismissRequest = { showDeleteConfirm = null },
                    title = { Text("¿Eliminar producto?") },
                    text = { Text("Esta acción eliminará '${showDeleteConfirm!!.nombre}' del inventario. ¿Deseas continuar?") },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.eliminarProducto(showDeleteConfirm!!.id!!)
                                showDeleteConfirm = null
                                productToEdit = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) { Text("Eliminar") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteConfirm = null }) { Text("Cancelar") }
                    }
                )
            }
        }
    }
}

@Composable
fun ProductItem(producto: Producto, onClick: () -> Unit) {
    val isDark = isSystemInDarkTheme()
    
    // Definición de estados de stock
    val stockState = when {
        producto.stockActual <= 0 -> "CRITICAL" // Sin stock
        producto.stockActual <= producto.stockMinimo -> "WARNING" // Stock mínimo o bajo
        else -> "NORMAL" // Stock suficiente
    }
    
    val (containerColor, borderColor, textColor, badgeText) = when (stockState) {
        "CRITICAL" -> {
            val container = if (isDark) Color(0xFF331010) else Color(0xFFFFEBEE)
            val border = if (isDark) Color(0xFFE57373) else Color(0xFFD32F2F)
            val text = if (isDark) Color(0xFFEF9A9A) else Color(0xFFC62828)
            val badge = "AGOTADO"
            Quadruple(container, border, text, badge)
        }
        "WARNING" -> {
            val container = if (isDark) Color(0xFF332B10) else Color(0xFFFFFDE7)
            val border = if (isDark) Color(0xFFFBC02D) else Color(0xFFF9A825)
            val text = if (isDark) Color(0xFFFFF59D) else Color(0xFFF57F17)
            val badge = "STOCK BAJO"
            Quadruple(container, border, text, badge)
        }
        else -> {
            val container = if (isDark) Color(0xFF103316) else Color(0xFFE8F5E9)
            val border = if (isDark) Color(0xFF81C784) else Color(0xFF4CAF50)
            val text = if (isDark) Color(0xFFA5D6A7) else Color(0xFF2E7D32)
            val badge = "EN STOCK"
            Quadruple(container, border, text, badge)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(producto.nombre, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = borderColor,
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Text(
                            badgeText,
                            color = if (stockState == "WARNING" && !isDark) Color.Black else Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(
                    producto.descripcion ?: "Sin descripción", 
                    style = MaterialTheme.typography.bodySmall, 
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column {
                        Text("Precio Venta", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$${String.format("%,.0f", producto.precioVenta)}", color = textColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Column {
                        Text("Precio Costo", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$${String.format("%,.0f", producto.precioCosto)}", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                    }
                }
            }
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Existencias", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = producto.stockActual.toString(),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    color = textColor
                )
            }
        }
    }
}

// Clase auxiliar para manejar los 4 valores de estilo
data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun AddProductForm(
    productToEdit: Producto? = null,
    onDismiss: () -> Unit,
    onDelete: () -> Unit = {},
    onConfirm: (String, String, Double, Double, Int, Int) -> Unit
) {
    var nombre by remember { mutableStateOf(productToEdit?.nombre ?: "") }
    var desc by remember { mutableStateOf(productToEdit?.descripcion ?: "") }
    var precioCosto by remember { mutableStateOf(productToEdit?.precioCosto?.let { String.format("%.0f", it) } ?: "") }
    var precioVenta by remember { mutableStateOf(productToEdit?.precioVenta?.let { String.format("%.0f", it) } ?: "") }
    var stockActual by remember { mutableStateOf(productToEdit?.stockActual?.toString() ?: "") }
    var stockMinimo by remember { mutableStateOf(productToEdit?.stockMinimo?.toString() ?: "") }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
            .verticalScroll(androidx.compose.foundation.rememberScrollState())
    ) {
        Text(
            text = if (productToEdit == null) "Nuevo Producto" else "Editar Producto",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
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
                label = { Text("Stock Actual") }, modifier = Modifier.weight(1f),
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
            Text(if (productToEdit == null) "Guardar Producto" else "Actualizar Producto")
        }
        
        if (productToEdit != null) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Eliminar")
                }
                
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancelar", color = Color.Gray)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(48.dp))
    }
}
