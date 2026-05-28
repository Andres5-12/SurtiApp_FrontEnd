package com.example.surtiapp.ui.screens

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.surtiapp.data.model.Transaccion
import com.example.surtiapp.ui.viewmodel.TransactionViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionHistoryScreen(viewModel: TransactionViewModel, onNavigateToCierre: () -> Unit) {
    val transacciones by viewModel.transacciones.collectAsState()
    val isLoading by viewModel.loading.collectAsState()
    val categorias by viewModel.categorias.collectAsState()
    val productos by viewModel.productos.collectAsState()
    val baseInicial by viewModel.baseInicial.collectAsState()
    
    val focusManager = LocalFocusManager.current
    
    var showDialog by remember { mutableStateOf(false) }
    var selectedType by remember { mutableStateOf("INGRESO") }
    var transactionToEdit by remember { mutableStateOf<Transaccion?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<Transaccion?>(null) }

    var filterDate by remember { mutableStateOf(LocalDate.now()) }
    var showDatePickerFilter by remember { mutableStateOf(false) }
    val datePickerFilterState = rememberDatePickerState(
        initialSelectedDateMillis = filterDate.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
    )

    if (showDatePickerFilter) {
        DatePickerDialog(
            onDismissRequest = { showDatePickerFilter = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerFilterState.selectedDateMillis?.let {
                        filterDate = Instant.ofEpochMilli(it).atZone(ZoneId.of("UTC")).toLocalDate()
                    }
                    showDatePickerFilter = false
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerFilter = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = datePickerFilterState)
        }
    }

    val filteredTransactions = remember(transacciones, filterDate) {
        transacciones.filter { tx ->
            try {
                val txFecha = tx.fecha ?: ""
                val txDate = if (txFecha.contains("T")) LocalDateTime.parse(txFecha).toLocalDate()
                             else LocalDate.parse(txFecha)
                txDate == filterDate
            } catch (e: Exception) {
                false
            }
        }
    }

    val ingresosEfectivo = filteredTransactions.filter { it.tipo == "INGRESO" && it.metodoPago == "EFECTIVO" }.sumOf { it.monto }
    val ingresosTransferencia = filteredTransactions.filter { it.tipo == "INGRESO" && it.metodoPago != "EFECTIVO" }.sumOf { it.monto }
    val egresos = filteredTransactions.filter { it.tipo == "EGRESO" }.sumOf { it.monto }
    val balance = (ingresosEfectivo + ingresosTransferencia) - egresos

    Scaffold(
        topBar = { 
            TopAppBar(
                title = { Text("Flujo de Caja") },
                actions = {
                    TextButton(onClick = { showDatePickerFilter = true }) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Cambiar Fecha")
                    }
                }
            ) 
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (filterDate == LocalDate.now()) "Hoy" else filterDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onNavigateToCierre) {
                    Icon(Icons.Default.Lock, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Cerrar Caja")
                }
            }

            // Campo para Base Inicial
            OutlinedTextField(
                value = if (baseInicial == 0.0) "" else baseInicial.toInt().toString(),
                onValueChange = { viewModel.updateBaseInicial(it.toDoubleOrNull() ?: 0.0) },
                label = { Text("Base Inicial de Caja (Efectivo)") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                prefix = { Text("$") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { 
                        viewModel.guardarBaseInicial()
                        focusManager.clearFocus() 
                    }
                ),
                leadingIcon = { Icon(Icons.Default.Store, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { 
                        viewModel.guardarBaseInicial()
                        focusManager.clearFocus() 
                    }) {
                        Icon(
                            imageVector = if (baseInicial != 0.0) Icons.Default.CheckCircle else Icons.Default.AddCircle, 
                            contentDescription = "Confirmar", 
                            tint = if (baseInicial != 0.0) Color(0xFF2E7D32) else Color.Gray
                        )
                    }
                }
            )

            BalanceCard(ingresosEfectivo, ingresosTransferencia, egresos, balance)
            
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val isDark = isSystemInDarkTheme()
                val btnIngreso = if (isDark) Color(0xFF388E3C) else Color(0xFF2E7D32)
                val btnGasto = if (isDark) Color(0xFFD32F2F) else Color(0xFFC62828)

                Button(
                    onClick = { 
                        selectedType = "INGRESO"
                        transactionToEdit = null
                        showDialog = true 
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = btnIngreso),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Ingreso")
                }
                
                Button(
                    onClick = { 
                        selectedType = "EGRESO"
                        transactionToEdit = null
                        showDialog = true 
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = btnGasto),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(Icons.Default.ArrowDownward, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Gasto")
                }
            }

            if (isLoading && transacciones.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else {
                if (filteredTransactions.isEmpty()) {
                    Box(Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                        Text("No hay movimientos este día", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().weight(1f),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredTransactions.sortedByDescending { it.fecha }) { tx ->
                            TransactionItem(
                                tx = tx,
                                onEdit = {
                                    selectedType = tx.tipo
                                    transactionToEdit = tx
                                    showDialog = true
                                },
                                onDelete = { showDeleteConfirm = tx }
                            )
                        }
                    }
                }
            }
        }

        if (showDialog) {
            AddTransactionDialog(
                categorias = categorias.filter { it.tipo == (transactionToEdit?.tipo ?: selectedType) },
                productos = productos,
                tipo = transactionToEdit?.tipo ?: selectedType,
                fixedDate = filterDate,
                transactionToEdit = transactionToEdit,
                onDismiss = { 
                    showDialog = false
                    transactionToEdit = null
                },
                onConfirm = { desc, monto, catId, esFiado, fecha, prodId, cant, metodo ->
                    viewModel.registrarMovimiento(
                        descripcion = desc, 
                        monto = monto, 
                        tipo = transactionToEdit?.tipo ?: selectedType, 
                        categoriaId = catId, 
                        esFiado = esFiado, 
                        fecha = fecha,
                        productoId = prodId,
                        cantidad = cant,
                        metodoPago = metodo,
                        id = transactionToEdit?.id
                    )
                    showDialog = false
                    transactionToEdit = null
                }
            )
        }

        if (showDeleteConfirm != null) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = null },
                title = { Text("¿Eliminar movimiento?") },
                text = { Text("Esta acción no se puede deshacer. ¿Estás seguro de eliminar '${showDeleteConfirm!!.descripcion}'?") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.eliminarMovimiento(showDeleteConfirm!!.id!!)
                            showDeleteConfirm = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) { Text("Eliminar") }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = null }) { Text("Cancelar") }
                }
            )
        }
    }
}

@Composable
fun BalanceCard(ingEfectivo: Double, ingTransferencia: Double, egresos: Double, balance: Double) {
    val isDark = isSystemInDarkTheme()
    val positiveColor = if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32)
    val negativeColor = if (isDark) Color(0xFFE57373) else Color(0xFFC62828)
    val transferColor = if (isDark) Color(0xFF64B5F6) else Color(0xFF1976D2)

    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Balance Neto", style = MaterialTheme.typography.labelMedium)
            Text(
                "$${String.format("%,.0f", balance)}",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
                color = if (balance >= 0) positiveColor else negativeColor
            )
            Spacer(modifier = Modifier.height(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    BalanceInfoItem("Efectivo", ingEfectivo, positiveColor, Icons.Default.Payments)
                    BalanceInfoItem("Transferencia", ingTransferencia, transferColor, Icons.Default.AccountBalance)
                    BalanceInfoItem("Egresos", egresos, negativeColor, Icons.Default.ArrowDownward)
                }
            }
        }
    }
}

@Composable
fun BalanceInfoItem(label: String, amount: Double, color: Color, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("$${String.format("%,.0f", amount)}", color = color, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun TransactionItem(tx: Transaccion, onEdit: () -> Unit, onDelete: () -> Unit) {
    val isDark = isSystemInDarkTheme()
    val positiveColor = if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32)
    val negativeColor = if (isDark) Color(0xFFE57373) else Color(0xFFC62828)
    val pendingContainer = if (isDark) Color(0xFF424200) else Color(0xFFFFF176)
    val pendingText = if (isDark) Color(0xFFFFF176) else Color(0xFF424200)

    val fechaParsed = try {
        LocalDateTime.parse(tx.fecha).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
    } catch (e: Exception) {
        tx.fecha ?: ""
    }
    var showMenu by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(tx.descripcion, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f, fill = false))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(tx.categoria.nombre, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(8.dp))
                    Text(fechaParsed, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                }
                if (tx.estado == "PENDIENTE") {
                    Surface(
                        color = pendingContainer,
                        shape = MaterialTheme.shapes.extraSmall,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            "Por Cobrar / Fiao", 
                            modifier = Modifier.padding(horizontal = 4.dp), 
                            fontSize = 10.sp, 
                            fontWeight = FontWeight.Bold,
                            color = pendingText
                        )
                    }
                }
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Opciones")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Editar") },
                            onClick = { showMenu = false; onEdit() },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Eliminar", color = MaterialTheme.colorScheme.error) },
                            onClick = { showMenu = false; onDelete() },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                        )
                    }
                }
                Text(
                    text = "${if (tx.tipo == "INGRESO") "+" else "-"}$${String.format("%,.0f", tx.monto)}",
                    color = if (tx.tipo == "INGRESO") positiveColor else negativeColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(
    categorias: List<com.example.surtiapp.data.model.Categoria>,
    productos: List<com.example.surtiapp.data.model.Producto>,
    tipo: String,
    fixedDate: LocalDate,
    transactionToEdit: Transaccion? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, Long, Boolean, LocalDateTime, Long?, Int, String) -> Unit
) {
    val defaultCat = remember(categorias, tipo) {
        if (tipo == "INGRESO") {
            categorias.find { it.nombre.contains("Ventas de mostrador", ignoreCase = true) } ?: categorias.firstOrNull()
        } else categorias.firstOrNull()
    }

    var nombreField by remember { mutableStateOf(transactionToEdit?.descripcion ?: "") }
    var montoField by remember { mutableStateOf(transactionToEdit?.monto?.let { String.format("%.0f", it) } ?: "") }
    var selectedCat by remember { mutableStateOf<com.example.surtiapp.data.model.Categoria?>(transactionToEdit?.categoria ?: defaultCat) }
    var esFiado by remember { mutableStateOf(transactionToEdit?.estado == "PENDIENTE") }
    
    var metodoPago by remember { mutableStateOf(transactionToEdit?.metodoPago ?: "EFECTIVO") }
    var showMetodoTransferencia by remember { mutableStateOf(metodoPago != "EFECTIVO") }
    
    // Si estamos editando un ingreso, intentamos encontrar el producto por nombre en la descripción
    var selectedProduct by remember { 
        mutableStateOf<com.example.surtiapp.data.model.Producto?>(
            if (tipo == "INGRESO" && transactionToEdit != null) {
                productos.find { transactionToEdit.descripcion.contains(it.nombre) }
            } else null
        ) 
    }
    
    var cantidad by remember { 
        mutableStateOf(
            if (tipo == "INGRESO" && transactionToEdit != null && selectedProduct != null) {
                val cant = (transactionToEdit.monto / selectedProduct!!.precioVenta).toInt()
                if (cant > 0) cant.toString() else "1"
            } else "1"
        ) 
    }
    
    var catExpanded by remember { mutableStateOf(false) }
    var prodExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(tipo, defaultCat) {
        if (tipo == "INGRESO" && transactionToEdit == null) {
            selectedCat = defaultCat
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (transactionToEdit != null) "Editar Movimiento" else if (tipo == "INGRESO") "Nuevo Ingreso" else "Nuevo Gasto") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Cuadro informativo de fecha
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Color.Gray)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Fecha del registro", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            val dateToFormat = try { 
                                transactionToEdit?.fecha?.let { LocalDateTime.parse(it).toLocalDate() } ?: fixedDate 
                            } catch(e:Exception) { fixedDate }
                            Text(dateToFormat.format(DateTimeFormatter.ofPattern("dd 'de' MMMM, yyyy")), fontWeight = FontWeight.Medium)
                        }
                    }
                }

                // Selector de Categoría (Siempre visible)
                ExposedDropdownMenuBox(expanded = catExpanded, onExpandedChange = { catExpanded = !catExpanded }) {
                    OutlinedTextField(
                        value = selectedCat?.nombre ?: "Seleccionar Categoría",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Categoría") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }) {
                        categorias.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.nombre) },
                                onClick = { selectedCat = cat; catExpanded = false }
                            )
                        }
                    }
                }

                if (tipo == "INGRESO") {
                    // MODO VENTA DE PRODUCTO
                    ExposedDropdownMenuBox(expanded = prodExpanded, onExpandedChange = { prodExpanded = !prodExpanded }) {
                        OutlinedTextField(
                            value = selectedProduct?.nombre ?: "Seleccionar Producto",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Producto") },
                            leadingIcon = { Icon(Icons.Default.ShoppingCart, contentDescription = null) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = prodExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            supportingText = {
                                selectedProduct?.let {
                                    Text("Stock disponible: ${it.stockActual} unidades", color = if (it.stockActual <= it.stockMinimo) Color.Red else Color.Gray)
                                }
                            }
                        )
                        ExposedDropdownMenu(expanded = prodExpanded, onDismissRequest = { prodExpanded = false }) {
                            productos.forEach { prod ->
                                val isOutOfStock = prod.stockActual <= 0
                                DropdownMenuItem(
                                    text = { 
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(prod.nombre)
                                            Text(
                                                "Stock: ${prod.stockActual}", 
                                                color = if (isOutOfStock) Color.Red else Color.Gray,
                                                fontSize = 12.sp
                                            )
                                        }
                                    },
                                    onClick = {
                                        if (!isOutOfStock) {
                                            selectedProduct = prod
                                            montoField = (prod.precioVenta * (cantidad.toDoubleOrNull() ?: 1.0)).toString()
                                            prodExpanded = false
                                        }
                                    },
                                    enabled = !isOutOfStock
                                )
                            }
                        }
                    }
                    
                    val cantInt = cantidad.toIntOrNull() ?: 0
                    val isInsufficent = selectedProduct != null && cantInt > selectedProduct!!.stockActual

                    OutlinedTextField(
                        value = cantidad,
                        onValueChange = { 
                            if (it.all { c -> c.isDigit() }) {
                                cantidad = it
                                selectedProduct?.let { p -> montoField = (p.precioVenta * (it.toDoubleOrNull() ?: 0.0)).toString() }
                            }
                        },
                        label = { Text("Cantidad") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = isInsufficent,
                        supportingText = {
                            if (isInsufficent) {
                                Text("No hay suficiente stock disponible", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    )
                } else {
                    // MODO GASTO
                    OutlinedTextField(
                        value = nombreField, onValueChange = { nombreField = it }, 
                        label = { Text("Nombre del gasto") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Monto Total (Siempre visible, calculado para ingresos)
                OutlinedTextField(
                    value = montoField, 
                    onValueChange = { if (tipo != "INGRESO") montoField = it }, 
                    label = { Text("Monto Total ($)") }, 
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = tipo == "INGRESO",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    prefix = { Text("$") }
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = esFiado, onCheckedChange = { esFiado = it })
                    Text(if (tipo == "INGRESO") "Es un fiado (Por cobrar)" else "Es un fiado (Por pagar)")
                }

                if (!esFiado) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text("Método de pago:", style = MaterialTheme.typography.labelLarge)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = !showMetodoTransferencia, onClick = { 
                            showMetodoTransferencia = false
                            metodoPago = "EFECTIVO"
                        })
                        Text("Efectivo")
                        Spacer(modifier = Modifier.width(16.dp))
                        RadioButton(selected = showMetodoTransferencia, onClick = { 
                            showMetodoTransferencia = true
                            if (metodoPago == "EFECTIVO") metodoPago = "NEQUI"
                        })
                        Text("Transferencia")
                    }

                    if (showMetodoTransferencia) {
                        var metodoExpanded by remember { mutableStateOf(false) }
                        val metodos = listOf("NEQUI", "DAVIPLATA", "DATAFONO")
                        
                        ExposedDropdownMenuBox(
                            expanded = metodoExpanded,
                            onExpandedChange = { metodoExpanded = !metodoExpanded }
                        ) {
                            OutlinedTextField(
                                value = metodoPago,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Seleccionar Transferencia") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = metodoExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = metodoExpanded,
                                onDismissRequest = { metodoExpanded = false }
                            ) {
                                metodos.forEach { opcion ->
                                    DropdownMenuItem(
                                        text = { Text(opcion) },
                                        onClick = {
                                            metodoPago = opcion
                                            metodoExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            val cantInt = cantidad.toIntOrNull() ?: 0
            val hasEnoughStock = selectedProduct == null || cantInt <= selectedProduct!!.stockActual
            
            Button(
                onClick = { 
                    val finalDate = if (transactionToEdit != null) {
                        try { LocalDateTime.parse(transactionToEdit.fecha) } catch(e:Exception) { LocalDateTime.now() }
                    } else {
                        fixedDate.atTime(LocalDateTime.now().toLocalTime())
                    }
                    
                    val finalDesc = if (tipo == "INGRESO") {
                        val unidadStr = if (cantidad.toIntOrNull() == 1) "unidad" else "unidades"
                        "${selectedProduct?.nombre ?: "Producto"} ($cantidad $unidadStr)"
                    } else {
                        nombreField
                    }
                    onConfirm(
                        finalDesc, 
                        montoField.toDoubleOrNull() ?: 0.0, 
                        selectedCat?.id ?: 0L, 
                        esFiado, 
                        finalDate,
                        selectedProduct?.id,
                        cantidad.toIntOrNull() ?: 0,
                        if (esFiado) "EFECTIVO" else metodoPago
                    ) 
                },
                enabled = montoField.isNotBlank() && selectedCat != null && hasEnoughStock && (
                    (tipo == "INGRESO" && selectedProduct != null) ||
                    (tipo == "EGRESO" && nombreField.isNotBlank())
                )
            ) { Text(if (transactionToEdit != null) "Guardar Cambios" else "Registrar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
