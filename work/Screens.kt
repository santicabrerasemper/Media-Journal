@file:OptIn(
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.example.mediajournal

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    onTypeSelected: (ContentType?) -> Unit,
    onStatusSelected: (ContentStatus?) -> Unit,
    onSearchChanged: (String) -> Unit,
    onAdd: () -> Unit,
    onOpen: (Long) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Media Journal") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) { Text("+") }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = onSearchChanged,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Buscar titulo") }
                )
            }
            item {
                Text("Tipo de contenido", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                FilterRow(
                    allLabel = "Todo",
                    selected = state.selectedType,
                    values = ContentType.entries,
                    label = { it.label },
                    onSelected = onTypeSelected
                )
            }
            item {
                StatusDropdown(
                    selectedStatus = state.selectedStatus,
                    onStatusSelected = onStatusSelected
                )
            }
            item {
                Text(
                    text = "Biblioteca",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            if (state.contents.isEmpty()) {
                item {
                    EmptyState()
                }
            } else {
                items(state.contents, key = { it.id }) { content ->
                    ContentCard(content = content, onClick = { onOpen(content.id) })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> FilterRow(
    allLabel: String,
    selected: T?,
    values: List<T>,
    label: (T) -> String,
    onSelected: (T?) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selected == null,
            onClick = { onSelected(null) },
            label = { Text(allLabel) }
        )
        values.forEach { value ->
            FilterChip(
                selected = selected == value,
                onClick = { onSelected(value) },
                label = { Text(label(value)) }
            )
        }
    }
}

@Composable
fun ContentCard(content: TrackedContent, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = typeColor(content.type).copy(alpha = 0.14f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TypeBadge(content.type)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = content.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${content.type.label} - ${content.status.label}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            RatingPill(content.rating)
        }
    }
}

@Composable
fun TypeBadge(type: ContentType) {
    val color = typeColor(type)
    val textColor = typeTextColor(type)
    Box(
        modifier = Modifier
            .size(42.dp)
            .background(color.copy(alpha = 0.28f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = when (type) {
                ContentType.SERIES -> "S"
                ContentType.ANIME -> "A"
                ContentType.MOVIE -> "P"
                ContentType.BOOK -> "L"
            },
            color = textColor,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun RatingText(rating: Int?) {
    Text(
        text = rating?.let { "$it/5" } ?: "-",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold
    )
}

@Composable
fun RatingPill(rating: Int?) {
    Box(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.78f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = rating?.let { "$it/5" } ?: "-",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("No hay contenido para estos filtros", style = MaterialTheme.typography.titleMedium)
        Text("Agrega una serie, pelicula o libro para empezar.")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditContentScreen(
    state: EditUiState,
    onTitleChanged: (String) -> Unit,
    onTypeChanged: (ContentType) -> Unit,
    onStatusChanged: (ContentStatus) -> Unit,
    onRatingChanged: (Int?) -> Unit,
    onGenreChanged: (String) -> Unit,
    onNotesChanged: (String) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    LaunchedEffect(state.saved) {
        if (state.saved) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.id == 0L) "Agregar contenido" else "Editar contenido") },
                navigationIcon = { BackChevron(onClick = onBack) }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                OutlinedTextField(
                    value = state.title,
                    onValueChange = onTitleChanged,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = state.titleError,
                    label = { Text("Titulo") },
                    supportingText = { if (state.titleError) Text("El titulo es obligatorio") }
                )
            }
            if (!state.isTypeLocked) {
                item {
                    Text("Tipo", fontWeight = FontWeight.SemiBold)
                    SelectableRow(ContentType.entries, state.type, { it.label }, onTypeChanged)
                }
            }
            item {
                Text("Estado", fontWeight = FontWeight.SemiBold)
                SelectableRow(ContentStatus.entries, state.status, { it.label }, onStatusChanged)
            }
            item {
                Text("Calificacion", fontWeight = FontWeight.SemiBold)
                RatingSelector(state.rating, onRatingChanged)
            }
            item {
                GenreDropdown(
                    type = state.type,
                    selectedGenre = state.genre,
                    onGenreChanged = onGenreChanged
                )
            }
            item {
                OutlinedTextField(
                    value = state.notes,
                    onValueChange = onNotesChanged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    label = { Text("Notas") }
                )
            }
            item {
                Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) {
                    Text("Guardar")
                }
            }
        }
    }
}

@Composable
fun GenreDropdown(
    type: ContentType,
    selectedGenre: String,
    onGenreChanged: (String) -> Unit
) {
    var expanded by remember(type) { mutableStateOf(false) }
    val genres = genresFor(type)

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedGenre,
            onValueChange = {},
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            readOnly = true,
            singleLine = true,
            label = { Text("Genero") },
            placeholder = { Text("Seleccionar genero") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            genres.forEach { genre ->
                DropdownMenuItem(
                    text = { Text(genre) },
                    onClick = {
                        onGenreChanged(genre)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun StatusDropdown(
    selectedStatus: ContentStatus?,
    onStatusSelected: (ContentStatus?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val label = selectedStatus?.label ?: "Todos"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = label,
            onValueChange = {},
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            readOnly = true,
            singleLine = true,
            label = { Text("Estado") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Todos") },
                onClick = {
                    onStatusSelected(null)
                    expanded = false
                }
            )
            ContentStatus.entries.forEach { status ->
                DropdownMenuItem(
                    text = { Text(status.label) },
                    onClick = {
                        onStatusSelected(status)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun <T> SelectableRow(
    values: List<T>,
    selected: T,
    label: (T) -> String,
    onSelected: (T) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 8.dp)
    ) {
        values.forEach { value ->
            if (value == selected) {
                Button(onClick = { onSelected(value) }) { Text(label(value)) }
            } else {
                OutlinedButton(onClick = { onSelected(value) }) { Text(label(value)) }
            }
        }
    }
}

@Composable
fun RatingSelector(rating: Int?, onSelected: (Int?) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 8.dp)
    ) {
        OutlinedButton(onClick = { onSelected(null) }) { Text("Sin nota") }
        (1..5).forEach { value ->
            if (rating == value) {
                Button(onClick = { onSelected(value) }) { Text("$value") }
            } else {
                OutlinedButton(onClick = { onSelected(value) }) { Text("$value") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    state: DetailUiState,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onDeleted: () -> Unit,
    onDelete: () -> Unit
) {
    LaunchedEffect(state.deleted) {
        if (state.deleted) onDeleted()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle") },
                navigationIcon = { BackChevron(onClick = onBack) }
            )
        }
    ) { padding ->
        val content = state.content
        if (content == null) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Cargando...")
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    TypeBadge(content.type)
                    Column {
                        Text(content.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("${content.type.label} - ${content.status.label}")
                    }
                }
                InfoLine("Calificacion", content.rating?.let { "$it/5" } ?: "Sin calificar")
                InfoLine("Genero", content.genre ?: "-")
                InfoLine("Inicio", content.startDate?.toString() ?: "-")
                InfoLine("Finalizacion", content.finishedDate?.toString() ?: "-")
                Text("Notas", fontWeight = FontWeight.SemiBold)
                Text(content.notes ?: "Sin notas")
                Spacer(Modifier.weight(1f))
                Button(onClick = { onEdit(content.id) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Editar")
                }
                OutlinedButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
                    Text("Eliminar")
                }
            }
        }
    }
}

@Composable
fun InfoLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}

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
                title = { Text("Estadisticas") },
                navigationIcon = { BackChevron(onClick = onBack) }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
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
                PieChart(
                    items = stats?.items.orEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    SummaryCard("Total", "${stats?.totalFinished ?: 0}", Modifier.weight(1f))
                    SummaryCard("Promedio", stats?.averageRating?.let { "%.1f".format(it) } ?: "-", Modifier.weight(1f))
                }
            }
            item {
                Text("Distribucion", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            items(stats?.items.orEmpty()) { item ->
                LegendRow(item)
            }
            item {
                Text("Terminados", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            if (stats?.finishedContents.isNullOrEmpty()) {
                item { Text("No hay contenido terminado en este mes.") }
            } else {
                items(stats!!.finishedContents) { content ->
                    ContentCard(content = content, onClick = {})
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
                title = { Text("Historico") },
                navigationIcon = { BackChevron(onClick = onBack) }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                PieChart(
                    items = stats?.items.orEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    SummaryCard("Total", "${stats?.totalFinished ?: 0}", Modifier.weight(1f))
                    SummaryCard("Promedio", stats?.averageRating?.let { "%.1f".format(it) } ?: "-", Modifier.weight(1f))
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
                    ContentCard(content = content, onClick = {})
                }
            }
        }
    }
}

@Composable
fun BackChevron(onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text("<", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SummaryCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
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
            Canvas(modifier = Modifier.size(220.dp)) {
                var startAngle = -90f
                visible.forEach { item ->
                    val sweep = item.percentage * 360f
                    drawArc(
                        color = typeColor(item.type),
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        size = Size(size.width, size.height),
                        style = Stroke(width = 42.dp.toPx())
                    )
                    startAngle += sweep
                }
            }
            Text("${visible.sumOf { it.count }}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
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

fun typeColor(type: ContentType): Color {
    return when (type) {
        ContentType.SERIES -> Color(0xFF8BCDBB)
        ContentType.ANIME -> Color(0xFFFFC7DA)
        ContentType.MOVIE -> Color(0xFFFFB5A7)
        ContentType.BOOK -> Color(0xFFAEC6FF)
    }
}

fun typeTextColor(type: ContentType): Color {
    return when (type) {
        ContentType.SERIES -> Color(0xFF24675B)
        ContentType.ANIME -> Color(0xFF9A4463)
        ContentType.MOVIE -> Color(0xFF9B4E43)
        ContentType.BOOK -> Color(0xFF465FA8)
    }
}

fun genresFor(type: ContentType): List<String> {
    return when (type) {
        ContentType.SERIES -> listOf(
            "Accion",
            "Animacion",
            "Comedia",
            "Crimen",
            "Documental",
            "Drama",
            "Fantasia",
            "Misterio",
            "Reality",
            "Romance",
            "Sci-fi",
            "Suspenso",
            "Terror"
        )
        ContentType.ANIME -> listOf(
            "Accion",
            "Aventura",
            "Comedia",
            "Drama",
            "Fantasia",
            "Isekai",
            "Mecha",
            "Misterio",
            "Romance",
            "Seinen",
            "Shonen",
            "Slice of life",
            "Sobrenatural",
            "Suspenso",
            "Terror"
        )
        ContentType.MOVIE -> listOf(
            "Accion",
            "Animacion",
            "Aventura",
            "Biografica",
            "Comedia",
            "Crimen",
            "Documental",
            "Drama",
            "Fantasia",
            "Historica",
            "Musical",
            "Romance",
            "Sci-fi",
            "Suspenso",
            "Terror"
        )
        ContentType.BOOK -> listOf(
            "Autoayuda",
            "Biografia",
            "Ciencia",
            "Ciencia ficcion",
            "Clasico",
            "Ensayo",
            "Fantasia",
            "Filosofia",
            "Historia",
            "Misterio",
            "No ficcion",
            "Novela",
            "Poesia",
            "Romance",
            "Terror",
            "Thriller"
        )
    }
}
