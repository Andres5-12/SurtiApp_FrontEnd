package com.example.surtiapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.surtiapp.data.model.Transaccion
import com.example.surtiapp.ui.viewmodel.TransactionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionHistoryScreen(viewModel: TransactionViewModel) {
    val transacciones by viewModel.transacciones.collectAsState()
    val isLoading by viewModel.loading.collectAsState()
    val categorias by viewModel.categorias.collectAsState()
    
    var showDialog by remember { mutableStateOf(false) }

    val ingresos = transacciones.filter { it.tipo == "INGRESO" }.sumOf { it.monto }
    val egresos = transacciones.filter { it.tipo == "EGRESO" }.sumOf { it.monto }
    val balance = ingresos - egresos

    Scaffold(
        topBar = { TopAppBar(title = { Text("Flujo de Caja") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }, containerColor = MaterialTheme.colorScheme.primary) {
                Icon(Icons.Default.Add, contentDescription = "Nueva Transacción")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            BalanceCard(ingresos, egresos, balance)
            
            if (isLoading && transacciones.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(transacciones) { tx ->
                        TransactionItem(tx)
                    }
                }
            }
        }

        if (showDialog) {
            AddTransactionDialog(
                categorias = categorias,
                onDismiss = { showDialog = false },
                onConfirm = { desc, monto, tipo, catId, esFiado ->
                    viewModel.registrarMovimiento(desc, monto, tipo, catId, esFiado)
                    showDialog = false
                }
            )
        }
    }
}

@Composable
fun BalanceCard(ingresos: Double, egresos: Double, balance: Double) {
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
                color = if (balance >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                BalanceInfoItem("Ingresos", ingresos, Color(0xFF2E7D32), Icons.Default.ArrowUpward)
                BalanceInfoItem("Egresos", egresos, Color(0xFFC62828), Icons.Default.ArrowDownward)
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
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Text("$${String.format("%,.0f", amount)}", color = color, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun TransactionItem(tx: Transaccion) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(tx.descripcion, fontWeight = FontWeight.Bold)
                Text(tx.categoria.nombre, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                if (tx.estado == "PENDIENTE") {
                    Surface(
                        color = Color(0xFFFFF176),
                        shape = MaterialTheme.shapes.extraSmall,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text("Por Cobrar / Fiao", modifier = Modifier.padding(horizontal = 4.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            Text(
                text = "${if (tx.tipo == "INGRESO") "+" else "-"}$${String.format("%,.0f", tx.monto)}",
                color = if (tx.tipo == "INGRESO") Color(0xFF2E7D32) else Color(0xFFC62828),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(
    categorias: List<com.example.surtiapp.data.model.Categoria>,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, String, Long, Boolean) -> Unit
) {
    var desc by remember { mutableStateOf("") }
    var monto by remember { mutableStateOf("") }
    var tipo by remember { mutableStateOf("INGRESO") }
    var expanded by remember { mutableStateOf(false) }
    var selectedCat by remember { mutableStateOf<com.example.surtiapp.data.model.Categoria?>(null) }
    var esFiado by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo Movimiento") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Selector Tipo
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(selected = tipo == "INGRESO", onClick = { tipo = "INGRESO" }, shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)) {
                        Text("Ingreso")
                    }
                    SegmentedButton(selected = tipo == "EGRESO", onClick = { tipo = "EGRESO" }, shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)) {
                        Text("Egreso")
                    }
                }

                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Descripción") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = monto, onValueChange = { monto = it }, 
                    label = { Text("Monto ($)") }, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                // Dropdown Categorias
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(
                        value = selectedCat?.nombre ?: "Seleccionar Categoría",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        categorias.filter { it.tipo == tipo }.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.nombre) },
                                onClick = {
                                    selectedCat = cat
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = esFiado, onCheckedChange = { esFiado = it })
                    Text("Es un fiado (Pendiente)")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(desc, monto.toDoubleOrNull() ?: 0.0, tipo, selectedCat?.id ?: 0L, esFiado) },
                enabled = desc.isNotBlank() && monto.isNotBlank() && selectedCat != null
            ) {
                Text("Registrar")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
