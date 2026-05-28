package com.example.surtiapp.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.surtiapp.data.model.CierreCaja
import com.example.surtiapp.ui.viewmodel.TransactionViewModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month
import java.time.format.TextStyle
import java.util.*

import java.time.format.DateTimeFormatter

fun parseFecha(fecha: String?): LocalDateTime {
    if (fecha.isNullOrBlank()) return LocalDateTime.now()
    val cleanFecha = fecha.trim()
    return try {
        when {
            cleanFecha.contains("T") -> {
                // Formato ISO: 2024-05-15T10:30:00 or 2024-05-15T10:30:00.000
                LocalDateTime.parse(cleanFecha)
            }
            cleanFecha.contains(" ") -> {
                // Formato: 2024-05-15 10:30:00
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                LocalDateTime.parse(cleanFecha, formatter)
            }
            cleanFecha.contains("/") -> {
                // Formato: 15/05/2024
                val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
                LocalDate.parse(cleanFecha, formatter).atStartOfDay()
            }
            else -> {
                // Formato: 2024-05-15
                LocalDate.parse(cleanFecha).atStartOfDay()
            }
        }
    } catch (e: Exception) {
        try {
            // Intento final con formateador flexible
            val formatter = DateTimeFormatter.ISO_DATE_TIME
            LocalDateTime.parse(cleanFecha, formatter)
        } catch (ex: Exception) {
            LocalDateTime.now()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BalanceGeneralScreen(viewModel: TransactionViewModel) {
    val cierres by viewModel.cierres.collectAsState()
    val isLoading by viewModel.loading.collectAsState()
    
    var selectedYear by remember { mutableIntStateOf(LocalDate.now().year) }
    var selectedMonth by remember { mutableStateOf(LocalDate.now().month) }
    var viewMode by remember { mutableStateOf("MENSUAL") } // "SEMANAL", "MENSUAL" o "ANUAL"

    val years = remember(cierres) {
        if (cierres.isEmpty()) listOf(LocalDate.now().year)
        else cierres.map { 
            try {
                if (it.fecha.contains("T")) LocalDateTime.parse(it.fecha).year 
                else LocalDate.parse(it.fecha).year
            } catch (e: Exception) {
                LocalDate.now().year
            }
        }.distinct().sortedDescending()
    }

    LaunchedEffect(Unit) {
        viewModel.cargarDatos()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Balance General") },
                actions = {
                    IconButton(onClick = { viewModel.cargarDatos() }) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Selector de Modo
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = viewMode == "SEMANAL",
                    onClick = { viewMode = "SEMANAL" },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                ) { Text("Semanal") }
                SegmentedButton(
                    selected = viewMode == "MENSUAL",
                    onClick = { viewMode = "MENSUAL" },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                ) { Text("Mensual") }
                SegmentedButton(
                    selected = viewMode == "ANUAL",
                    onClick = { viewMode = "ANUAL" },
                    shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                ) { Text("Anual") }
            }

            // Selectores de Fecha
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Año
                var yearExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = yearExpanded,
                    onExpandedChange = { yearExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = selectedYear.toString(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Año") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = yearExpanded) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = yearExpanded, onDismissRequest = { yearExpanded = false }) {
                        years.forEach { year ->
                            DropdownMenuItem(text = { Text(year.toString()) }, onClick = { selectedYear = year; yearExpanded = false })
                        }
                    }
                }

                if (viewMode == "MENSUAL") {
                    // Mes
                    var monthExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = monthExpanded,
                        onExpandedChange = { monthExpanded = it },
                        modifier = Modifier.weight(1.2f)
                    ) {
                        OutlinedTextField(
                            value = selectedMonth.getDisplayName(TextStyle.FULL, Locale("es")).replaceFirstChar { it.uppercase() },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Mes") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = monthExpanded) },
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(expanded = monthExpanded, onDismissRequest = { monthExpanded = false }) {
                            Month.values().forEach { month ->
                                DropdownMenuItem(
                                    text = { Text(month.getDisplayName(TextStyle.FULL, Locale("es")).replaceFirstChar { it.uppercase() }) },
                                    onClick = { selectedMonth = month; monthExpanded = false }
                                )
                            }
                        }
                    }
                }
            }

            if (isLoading && cierres.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                when (viewMode) {
                    "SEMANAL" -> WeeklyBalanceView(cierres)
                    "MENSUAL" -> MonthlyBalanceView(cierres, selectedYear, selectedMonth)
                    "ANUAL" -> AnnualBalanceView(cierres, selectedYear)
                }
            }
        }
    }
}

@Composable
fun WeeklyBalanceView(cierres: List<CierreCaja>) {
    val today = LocalDate.now()
    // Obtenemos los últimos 7 días
    val last7Days = (0..6).map { today.minusDays(it.toLong()) }.reversed()
    
    val cierresConFecha = remember(cierres) {
        cierres.map { it to parseFecha(it.fecha) }
    }

    val cierresSemana = cierresConFecha.filter { (_, dt) ->
        dt.toLocalDate().isAfter(today.minusDays(7)) || dt.toLocalDate().isEqual(today.minusDays(6))
    }

    val totalIngresos = cierresSemana.sumOf { (c, _) -> c.ingresosEfectivo + c.ingresosTransferencia }
    val totalEgresos = cierresSemana.sumOf { (c, _) -> c.egresos }
    val totalUtilidad = cierresSemana.sumOf { (c, _) -> c.utilidad ?: 0.0 }
    val balance = totalIngresos - totalEgresos

    BalanceStatsCards(totalIngresos, totalEgresos, balance, totalUtilidad)

    Spacer(Modifier.height(12.dp))
    Text("Balance de los Últimos 7 Días", fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 14.sp)
    
    val chartItems = last7Days.map { date ->
        val cierresDia = cierresSemana.filter { (_, dt) -> dt.toLocalDate().isEqual(date) }
        BarChartItem(
            label = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale("es")).uppercase(),
            ingreso = cierresDia.sumOf { (c, _) -> c.ingresosEfectivo + c.ingresosTransferencia },
            egreso = cierresDia.sumOf { (c, _) -> c.egresos }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            if (cierresSemana.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                    Text("No hay cierres en la última semana", color = Color.Gray)
                }
            } else {
                ChartLegend()
                EnhancedBarChart(chartItems, isFixed = true)
            }
        }
    }
}

@Composable
fun MonthlyBalanceView(cierres: List<CierreCaja>, year: Int, month: Month) {
    // Pre-procesamos los cierres para evitar parseos repetitivos
    val cierresConFecha = remember(cierres) {
        cierres.map { it to parseFecha(it.fecha) }
    }
    
    val cierresMes = cierresConFecha.filter { (_, dt) ->
        dt.year == year && dt.month == month
    }

    val totalIngresos = cierresMes.sumOf { (c, _) -> c.ingresosEfectivo + c.ingresosTransferencia }
    val totalEgresos = cierresMes.sumOf { (c, _) -> c.egresos }
    val totalUtilidad = cierresMes.sumOf { (c, _) -> c.utilidad ?: 0.0 }
    val balance = totalIngresos - totalEgresos

    BalanceStatsCards(totalIngresos, totalEgresos, balance, totalUtilidad)

    Spacer(Modifier.height(12.dp))
    Text("Balance por Semanas del Mes", fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 14.sp)
    
    val daysInMonth = month.length(java.time.Year.isLeap(year.toLong()))
    
    // Agrupamos los días en 4 o 5 semanas (intervalos de 7 días)
    val chartItems = mutableListOf<BarChartItem>()
    for (i in 0 until (daysInMonth + 6) / 7) {
        val startDay = i * 7 + 1
        val endDay = minOf((i + 1) * 7, daysInMonth)
        
        val cierresSemana = cierresMes.filter { (_, dt) -> 
            dt.dayOfMonth in startDay..endDay 
        }
        
        chartItems.add(
            BarChartItem(
                label = "S${i + 1} ($startDay-$endDay)",
                ingreso = cierresSemana.sumOf { (c, _) -> c.ingresosEfectivo + c.ingresosTransferencia },
                egreso = cierresSemana.sumOf { (c, _) -> c.egresos }
            )
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            if (cierresMes.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                    Text("No hay movimientos registrados para este mes", color = Color.Gray)
                }
            } else {
                ChartLegend()
                EnhancedBarChart(chartItems, isFixed = true)
            }
        }
    }
}

@Composable
fun AnnualBalanceView(cierres: List<CierreCaja>, year: Int) {
    val cierresConFecha = remember(cierres) {
        cierres.map { it to parseFecha(it.fecha) }
    }
    
    val cierresAño = cierresConFecha.filter { (_, dt) -> dt.year == year }
    
    val totalIngresos = cierresAño.sumOf { (c, _) -> c.ingresosEfectivo + c.ingresosTransferencia }
    val totalEgresos = cierresAño.sumOf { (c, _) -> c.egresos }
    val totalUtilidad = cierresAño.sumOf { (c, _) -> c.utilidad ?: 0.0 }
    val balance = totalIngresos - totalEgresos

    BalanceStatsCards(totalIngresos, totalEgresos, balance, totalUtilidad)

    Spacer(Modifier.height(12.dp))
    Text("Balance Mensual del Año", fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 14.sp)
    
    val chartItems = Month.entries.map { m ->
        val cierresMes = cierresAño.filter { (_, dt) -> dt.month == m }
        BarChartItem(
            label = m.getDisplayName(TextStyle.SHORT, Locale("es")).uppercase().take(3),
            ingreso = cierresMes.sumOf { (c, _) -> c.ingresosEfectivo + c.ingresosTransferencia },
            egreso = cierresMes.sumOf { (c, _) -> c.egresos }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            ChartLegend()
            EnhancedBarChart(chartItems, isFixed = true)
        }
    }
}

@Composable
fun BalanceStatsCards(ingresos: Double, egresos: Double, balance: Double, utilidadReal: Double) {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    
    val positiveColor = if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32)
    val negativeColor = if (isDark) Color(0xFFE57373) else Color(0xFFC62828)
    val profitColor = if (isDark) Color(0xFF64B5F6) else Color(0xFF1976D2)
    
    val positiveContainer = if (isDark) Color(0xFF1B3D1B) else Color(0xFFE8F5E9)
    val negativeContainer = if (isDark) Color(0xFF421C1C) else Color(0xFFFFEBEE)
    val profitContainer = if (isDark) Color(0xFF0D2D3E) else Color(0xFFE3F2FD)

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatCard("Ingresos", ingresos, positiveColor, Icons.Default.TrendingUp, Modifier.weight(1f))
        StatCard("Egresos", egresos, negativeColor, Icons.Default.TrendingDown, Modifier.weight(1f))
    }
    
    Spacer(Modifier.height(8.dp))
    
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        // Balance de Caja (Flujo de efectivo)
        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(containerColor = if (balance >= 0) positiveContainer else negativeContainer)
        ) {
            Column(Modifier.padding(12.dp)) {
                Text("Flujo de Caja", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Text(
                    "$${String.format("%,.0f", balance)}", 
                    fontSize = 18.sp, 
                    fontWeight = FontWeight.Bold,
                    color = if (balance >= 0) positiveColor else negativeColor
                )
            }
        }
        
        // GANANCIA REAL (Utilidad real tras COGS y gastos)
        Card(
            modifier = Modifier.weight(1.2f),
            colors = CardDefaults.cardColors(containerColor = profitContainer),
            border = BorderStroke(1.dp, profitColor.copy(alpha = 0.3f))
        ) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Ganancia Real", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text(
                        "$${String.format("%,.0f", utilidadReal)}", 
                        fontSize = 20.sp, 
                        fontWeight = FontWeight.Black, 
                        color = profitColor
                    )
                }
                Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = profitColor, modifier = Modifier.size(24.dp))
            }
        }
    }
}

@Composable
fun StatCard(label: String, amount: Double, color: Color, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(Modifier.padding(12.dp)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("$${String.format("%,.0f", amount)}", fontWeight = FontWeight.Bold, color = color)
        }
    }
}

data class BarChartItem(val label: String, val ingreso: Double, val egreso: Double)

@Composable
fun ChartLegend() {
    val isDark = isSystemInDarkTheme()
    val incomeColor = if (isDark) Color(0xFF81C784) else Color(0xFF4CAF50)
    val expenseColor = if (isDark) Color(0xFFE57373) else Color(0xFFF44336)
    
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(10.dp).background(incomeColor, RoundedCornerShape(2.dp)))
        Text(" Ingresos", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(16.dp))
        Box(Modifier.size(10.dp).background(expenseColor, RoundedCornerShape(2.dp)))
        Text(" Egresos", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun EnhancedBarChart(items: List<BarChartItem>, isFixed: Boolean = false) {
    val isDark = isSystemInDarkTheme()
    val maxVal = items.flatMap { listOf(it.ingreso, it.egreso) }.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
    
    val incomeColor = if (isDark) Color(0xFF81C784) else Color(0xFF4CAF50)
    val expenseColor = if (isDark) Color(0xFFE57373) else Color(0xFFF44336)
    
    Box(modifier = Modifier.fillMaxWidth().height(230.dp)) {
        Row(modifier = Modifier.fillMaxSize()) {
            // EJE Y ESTÁTICO (Guía de datos)
            Column(
                modifier = Modifier
                    .width(35.dp)
                    .fillMaxHeight()
                    .padding(bottom = 30.dp, top = 20.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                Text("${(maxVal/1000).toInt()}k", fontSize = 8.sp, color = Color.Gray)
                Text("${(maxVal/2000).toInt()}k", fontSize = 8.sp, color = Color.Gray)
                Text("0", fontSize = 8.sp, color = Color.Gray)
            }

            Spacer(Modifier.width(4.dp))

            // ÁREA DE GRÁFICA DESPLAZABLE
            val horizontalScrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .weight(1f)
                    .then(if (isFixed) Modifier else Modifier.horizontalScroll(horizontalScrollState))
            ) {
                // Contenedor de Barras con Scroll Vertical interno
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()) // Scroller vertical interno
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(180.dp),
                        horizontalArrangement = if (isFixed) Arrangement.SpaceEvenly else Arrangement.Start,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        items.forEachIndexed { index, item ->
                            val hasData = item.ingreso > 0 || item.egreso > 0
                            val barGroupWidth = 44.dp
                            
                            Column(
                                modifier = if (isFixed) Modifier.weight(1f) else Modifier.width(barGroupWidth),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom
                            ) {
                                if (hasData && (!isFixed || items.size <= 12)) {
                                    val totalVal = item.ingreso - item.egreso
                                    Text(
                                        text = if (totalVal >= 0) "+${(totalVal/1000).toInt()}k" else "${(totalVal/1000).toInt()}k",
                                        fontSize = 7.sp,
                                        color = if (totalVal >= 0) incomeColor else expenseColor
                                    )
                                }

                                Row(
                                    modifier = Modifier.height(150.dp).fillMaxWidth(),
                                    verticalAlignment = Alignment.Bottom,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(if (isFixed) 10.dp else 12.dp)
                                            .fillMaxHeight((item.ingreso / maxVal).toFloat().coerceIn(0.01f, 1f))
                                            .background(incomeColor.copy(alpha = if (item.ingreso > 0) 0.8f else 0.1f), RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                                    )
                                    Spacer(Modifier.width(2.dp))
                                    Box(
                                        modifier = Modifier
                                            .width(if (isFixed) 10.dp else 12.dp)
                                            .fillMaxHeight((item.egreso / maxVal).toFloat().coerceIn(0.01f, 1f))
                                            .background(expenseColor.copy(alpha = if (item.egreso > 0) 0.8f else 0.1f), RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                                    )
                                }
                            }
                            if (!isFixed) Spacer(Modifier.width(2.dp))
                        }
                    }
                }

                // FILA DE ETIQUETAS (Nombres) - Fija verticalmente bajo las barras
                Row(
                    modifier = Modifier.fillMaxWidth().height(25.dp),
                    horizontalArrangement = if (isFixed) Arrangement.SpaceEvenly else Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items.forEachIndexed { index, item ->
                        val shouldShowLabel = !isFixed || items.size <= 12 || index % 2 == 0
                        Box(
                            modifier = if (isFixed) Modifier.weight(1f) else Modifier.width(44.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (shouldShowLabel) {
                                Text(
                                    text = item.label,
                                    fontSize = 9.sp,
                                    color = if (item.ingreso > 0 || item.egreso > 0) MaterialTheme.colorScheme.onSurface else Color.LightGray,
                                    fontWeight = if (item.ingreso > 0 || item.egreso > 0) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1
                                )
                            }
                        }
                        if (!isFixed) Spacer(Modifier.width(2.dp))
                    }
                }
            }
        }
    }
}
