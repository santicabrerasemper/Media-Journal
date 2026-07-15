@file:OptIn(
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.example.mediajournal

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    state: StatsUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onBack: () -> Unit
) {
    val stats = state.stats
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Estad\u00edsticas") },
                navigationIcon = { BackChevron(onClick = onBack) }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = onPreviousMonth) { Text("<") }
                    Text(
                        text = monthLabel(state.selectedMonth, state.selectedYear),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedButton(onClick = onNextMonth) { Text(">") }
                }
            }
            item {
                ChartSection(items = stats?.items.orEmpty())
            }
            item {
                StatsNarrative(
                    total = stats?.totalFinished ?: 0,
                    average = stats?.averageRating,
                    emptyText = "Todavía no marcaste contenido como terminado este mes."
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    SummaryCard("Total", "${stats?.totalFinished ?: 0}", Modifier.weight(1f))
                    SummaryCard("Promedio /10", stats?.averageRating?.let { "%.1f".format(it) } ?: "-", Modifier.weight(1f))
                }
            }
            item {
                Text("Distribución por tipo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            item {
                TypeStatsGrid(items = stats?.items.orEmpty())
            }
            item {
                Text("Terminados", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            if (stats?.finishedContents.isNullOrEmpty()) {
                item { Text("No hay contenido terminado en este mes.") }
            } else {
                items(stats!!.finishedContents) { content ->
                    ContentCard(
                        content = content,
                        onClick = {},
                        onMarkFinished = {},
                        onStatusChanged = {},
                        onRatingChanged = {}
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoricalScreen(
    state: HistoricalUiState,
    onBack: () -> Unit
) {
    val stats = state.stats
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hist\u00f3rico") },
                navigationIcon = { BackChevron(onClick = onBack) }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ChartSection(items = stats?.items.orEmpty())
            }
            item {
                StatsNarrative(
                    total = stats?.totalFinished ?: 0,
                    average = stats?.averageRating,
                    emptyText = "Tu histórico se va a completar cuando termines contenido."
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    SummaryCard("Total", "${stats?.totalFinished ?: 0}", Modifier.weight(1f))
                    SummaryCard("Promedio /10", stats?.averageRating?.let { "%.1f".format(it) } ?: "-", Modifier.weight(1f))
                }
            }
            item {
                Text("Totales por tipo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            item {
                HistoricalTotals(items = stats?.items.orEmpty())
            }
            stats?.bestRated?.let { content ->
                item {
                    Text("Mejor calificado", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    ContentCard(
                        content = content,
                        onClick = {},
                        onMarkFinished = {},
                        onStatusChanged = {},
                        onRatingChanged = {}
                    )
                }
            }
        }
    }
}

@Composable
fun ChartSection(items: List<PieChartItem>) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        PieChart(
            items = items,
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
        )
    }
}

@Composable
fun StatsNarrative(total: Int, average: Double?, emptyText: String) {
    val text = if (total == 0) {
        emptyText
    } else {
        val averageText = average?.let { " con promedio %.1f/10".format(it) }.orEmpty()
        "Terminaste $total contenido${if (total == 1) "" else "s"}$averageText."
    }
    Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
fun SummaryCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun PieChart(items: List<PieChartItem>, modifier: Modifier = Modifier) {
    val visible = items.filter { it.count > 0 }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (visible.isEmpty()) {
            Text("Sin datos para este mes")
        } else {
            Canvas(modifier = Modifier.size(132.dp)) {
                var startAngle = -90f
                visible.forEach { item ->
                    val sweep = item.percentage * 360f
                    drawArc(
                        color = typeColor(item.type),
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        size = Size(size.width, size.height),
                        style = Stroke(width = 26.dp.toPx())
                    )
                    startAngle += sweep
                }
            }
            Text("${visible.sumOf { it.count }}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun TypeStatsGrid(items: List<PieChartItem>) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { item ->
            Card(
                modifier = Modifier.width(176.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = typeColor(item.type).copy(alpha = 0.16f))
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).background(typeColor(item.type), CircleShape))
                        Text(item.type.label, fontWeight = FontWeight.SemiBold)
                    }
                    Text("${item.count}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("${(item.percentage * 100).toInt()}%", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun LegendRow(item: PieChartItem) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(12.dp).background(typeColor(item.type), CircleShape))
            Text(item.type.label)
        }
        Text("${item.count} - ${(item.percentage * 100).toInt()}%")
    }
}

@Composable
fun HistoricalTotals(items: List<PieChartItem>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.forEach { item ->
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(12.dp).background(typeColor(item.type), CircleShape))
                        Text(item.type.label, fontWeight = FontWeight.SemiBold)
                    }
                    Text("${item.count}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

fun monthLabel(month: Int, year: Int): String {
    val name = Month.of(month).getDisplayName(TextStyle.FULL, Locale("es", "ES"))
    return name.replaceFirstChar { it.uppercase() } + " $year"
}

