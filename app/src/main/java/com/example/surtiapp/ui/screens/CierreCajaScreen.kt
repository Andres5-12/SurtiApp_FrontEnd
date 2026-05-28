package com.example.surtiapp.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import com.example.surtiapp.ui.viewmodel.TransactionViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CierreCajaScreen(viewModel: TransactionViewModel, onBack: () -> Unit) {
    val transacciones by viewModel.transacciones.collectAsState()
    val baseInicial by viewModel.baseInicial.collectAsState()
    val error by viewModel.error.collectAsState()
    
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        selectedDate = Instant.ofEpochMilli(it).atZone(ZoneId.of("UTC")).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    val filteredTransactions = remember(transacciones, selectedDate) {
        transacciones.filter { tx ->
            try {
                val txFecha = tx.fecha ?: ""
                val dt = if (txFecha.contains("T")) java.time.LocalDateTime.parse(txFecha).toLocalDate()
                         else java.time.LocalDate.parse(txFecha)
                dt == selectedDate
            } catch (e: Exception) {
                false
            }
        }
    }

    val ingEfectivo = filteredTransactions.filter { it.tipo == "INGRESO" && it.metodoPago == "EFECTIVO" && it.estado == "ACTIVO" }.sumOf { it.monto }
    val ingTransferencia = filteredTransactions.filter { it.tipo == "INGRESO" && it.metodoPago != "EFECTIVO" && it.estado == "ACTIVO" }.sumOf { it.monto }
    val egresos = filteredTransactions.filter { it.tipo == "EGRESO" && it.estado == "ACTIVO" }.sumOf { it.monto }
    
    // Cálculo de utilidad: (Ventas totales - Costo de mercancía - Gastos)
    val costoMercancia = filteredTransactions.filter { it.tipo == "INGRESO" && it.estado == "ACTIVO" }.sumOf { it.costoTotal ?: 0.0 }
    val utilidadDia = (ingEfectivo + ingTransferencia) - costoMercancia - egresos

    var saldoReal by remember { mutableStateOf("") }
    var baseConfirmada by remember { mutableStateOf("") }
    var observaciones by remember { mutableStateOf("") }

    val baseCoincide = baseConfirmada.toDoubleOrNull() == baseInicial
    
    // El saldo esperado de la operación (ventas - gastos)
    val saldoOperacionEsperado = ingEfectivo - egresos
    // El dinero real contado debería ser el recaudo del día (sin contar la base, ya que se confirmó aparte)
    val diferencia = (saldoReal.toDoubleOrNull() ?: 0.0) - saldoOperacionEsperado

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cierre de Caja Diario") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = null) }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Fecha y Encabezado
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                onClick = { showDatePicker = true }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Cierre para la fecha:", style = MaterialTheme.typography.labelSmall)
                        Text(
                            selectedDate.format(DateTimeFormatter.ofPattern("EEEE, dd 'de' MMMM")),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Icon(Icons.Default.CalendarToday, contentDescription = "Cambiar fecha")
                }
            }

            // Desglose de Ventas
            Text("Movimientos Registrados", fontWeight = FontWeight.Bold, color = Color.Gray)
            
            SummaryRow("Ventas en Efectivo (+)", ingEfectivo, Color(0xFF2E7D32))
            SummaryRow("Transferencias (+)", ingTransferencia, Color(0xFF1976D2))
            SummaryRow("Gastos Totales (-)", egresos, Color(0xFFC62828))
            
            Divider()

            // Entradas de Usuario
            Text("Validación de Efectivo", fontWeight = FontWeight.Bold)
            
            OutlinedTextField(
                value = baseConfirmada,
                onValueChange = { baseConfirmada = it },
                label = { Text("Confirmar Base de Caja") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                prefix = { Text("$") },
                supportingText = {
                    if (baseConfirmada.isNotBlank()) {
                        if (baseCoincide) {
                            Text("La base coincide correctamente", color = Color(0xFF2E7D32))
                        } else {
                            Text("La base no coincide con la registrada", color = Color.Red)
                        }
                    }
                },
                trailingIcon = {
                    if (baseConfirmada.isNotBlank()) {
                        Icon(
                            imageVector = if (baseCoincide) Icons.Default.CheckCircle else Icons.Default.Error,
                            contentDescription = null,
                            tint = if (baseCoincide) Color(0xFF2E7D32) else Color.Red
                        )
                    }
                }
            )

            OutlinedTextField(
                value = saldoReal,
                onValueChange = { saldoReal = it },
                label = { Text("Recaudo de Ventas en Efectivo") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                prefix = { Text("$") },
                supportingText = { Text("Ingresa solo el dinero de las ventas, sin la base inicial.") }
            )

            // Resultado del Cierre
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (diferencia == 0.0) 
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) 
                    else 
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                ),
                border = BorderStroke(
                    1.dp, 
                    if (diferencia == 0.0) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                )
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Ventas esperadas en Efectivo:", style = MaterialTheme.typography.bodyMedium)
                        Text("$${String.format("%,.0f", saldoOperacionEsperado)}", fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Diferencia (Sobrante/Faltante):", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = (if (diferencia > 0) "+" else "") + String.format("%,.0f", diferencia),
                            fontWeight = FontWeight.Black,
                            color = if (diferencia == 0.0) 
                                if (androidx.compose.foundation.isSystemInDarkTheme()) Color(0xFF81C784) else Color(0xFF2E7D32)
                            else 
                                MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            OutlinedTextField(
                value = observaciones,
                onValueChange = { observaciones = it },
                label = { Text("Observaciones o Novedades") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            Button(
                onClick = {
                    viewModel.realizarCierre(
                        ingEfectivo = ingEfectivo,
                        ingTransferencia = ingTransferencia,
                        egresos = egresos,
                        saldoInicial = baseInicial,
                        saldoReal = saldoReal.toDoubleOrNull() ?: 0.0,
                        obs = observaciones,
                        utilidad = utilidadDia
                    )
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = MaterialTheme.shapes.medium,
                enabled = (saldoReal.isNotBlank() && baseCoincide) || (filteredTransactions.isEmpty() && saldoReal.isNotBlank())
            ) {
                Icon(Icons.Default.Lock, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Cerrar Caja ${selectedDate.format(DateTimeFormatter.ofPattern("dd/MM"))}")
            }
            
            if (error != null) {
                Text(error!!, color = Color.Red, modifier = Modifier.padding(top = 8.dp))
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun SummaryRow(label: String, amount: Double, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label)
        Text("$${String.format("%,.0f", amount)}", color = color, fontWeight = FontWeight.Bold)
    }
}
