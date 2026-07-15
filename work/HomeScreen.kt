@file:OptIn(
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.example.mediajournal

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.ui.draw.clip
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
fun HomeScreen(
    state: HomeUiState,
    onTypeSelected: (ContentType?) -> Unit,
    onStatusSelected: (ContentStatus?) -> Unit,
    onGenreSelected: (String?) -> Unit,
    onSortSelected: (HomeSort) -> Unit,
    onSearchChanged: (String) -> Unit,
    onAdd: () -> Unit,
    onMarkFinished: (TrackedContent) -> Unit,
    onStatusChanged: (TrackedContent, ContentStatus) -> Unit,
    onRatingChanged: (TrackedContent, Int?) -> Unit,
    onOpen: (Long) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(
                            text = "Media Journal",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Series, pelis y libros en un solo lugar",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            val selectedType = state.selectedType
            if (selectedType == null) {
                FloatingActionButton(onClick = onAdd) { Text("+") }
            } else {
                ExtendedFloatingActionButton(
                    onClick = onAdd,
                    icon = { Text("+") },
                    text = { Text(selectedType.singleLabel()) }
                )
            }
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
                HomeSummaryHeader(summary = state.summary)
            }
            if (state.inProgressContents.isNotEmpty()) {
                item {
                    CurrentSection(
                        contents = state.inProgressContents,
                        onOpen = onOpen,
                        onMarkFinished = onMarkFinished,
                        onStatusChanged = onStatusChanged,
                        onRatingChanged = onRatingChanged
                    )
                }
            }
            item {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = onSearchChanged,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Buscar título") }
                )
            }
            item {
                HomeFilterBar(
                    state = state,
                    onTypeSelected = onTypeSelected,
                    onStatusSelected = onStatusSelected,
                    onGenreSelected = onGenreSelected,
                    onSortSelected = onSortSelected
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
                    ContentCard(
                        content = content,
                        onClick = { onOpen(content.id) },
                        onMarkFinished = { onMarkFinished(content) },
                        onStatusChanged = { onStatusChanged(content, it) },
                        onRatingChanged = { onRatingChanged(content, it) }
                    )
                }
            }
        }
    }
}

@Composable
fun HomeFilterBar(
    state: HomeUiState,
    onTypeSelected: (ContentType?) -> Unit,
    onStatusSelected: (ContentStatus?) -> Unit,
    onGenreSelected: (String?) -> Unit,
    onSortSelected: (HomeSort) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TypeTabs(selected = state.selectedType, onSelected = onTypeSelected)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CompactStatusDropdown(
                selectedStatus = state.selectedStatus,
                onStatusSelected = onStatusSelected,
                modifier = Modifier.weight(1f)
            )
            GenreFilterDropdown(
                genres = state.availableGenres,
                selectedGenre = state.selectedGenre,
                onGenreSelected = onGenreSelected,
                modifier = Modifier.weight(1f)
            )
        }
        SortDropdown(
            selectedSort = state.selectedSort,
            onSortSelected = onSortSelected,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun CompactStatusDropdown(
    selectedStatus: ContentStatus?,
    onStatusSelected: (ContentStatus?) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedStatus?.label ?: "Todos",
            onValueChange = {},
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            readOnly = true,
            singleLine = true,
            label = { Text("Estado") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("Todos") }, onClick = {
                onStatusSelected(null)
                expanded = false
            })
            ContentStatus.entries.forEach { status ->
                DropdownMenuItem(text = { Text(status.label) }, onClick = {
                    onStatusSelected(status)
                    expanded = false
                })
            }
        }
    }
}

@Composable
fun GenreFilterDropdown(
    genres: List<String>,
    selectedGenre: String?,
    onGenreSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (genres.isNotEmpty()) expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedGenre ?: "Todos",
            onValueChange = {},
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            enabled = genres.isNotEmpty(),
            readOnly = true,
            singleLine = true,
            label = { Text("Género") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Todos") },
                onClick = {
                    onGenreSelected(null)
                    expanded = false
                }
            )
            genres.forEach { genre ->
                DropdownMenuItem(
                    text = { Text(genre) },
                    onClick = {
                        onGenreSelected(genre)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun SortDropdown(
    selectedSort: HomeSort,
    onSortSelected: (HomeSort) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedSort.label,
            onValueChange = {},
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            readOnly = true,
            singleLine = true,
            label = { Text("Orden") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            HomeSort.entries.forEach { sort ->
                DropdownMenuItem(
                    text = { Text(sort.label) },
                    onClick = {
                        onSortSelected(sort)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun CurrentSection(
    contents: List<TrackedContent>,
    onOpen: (Long) -> Unit,
    onMarkFinished: (TrackedContent) -> Unit,
    onStatusChanged: (TrackedContent, ContentStatus) -> Unit,
    onRatingChanged: (TrackedContent, Int?) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Actualmente viendo/leyendo",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            contents.forEach { content ->
                CurrentContentCard(
                    content = content,
                    onClick = { onOpen(content.id) },
                    onMarkFinished = { onMarkFinished(content) },
                    onStatusChanged = { onStatusChanged(content, it) },
                    onRatingChanged = { onRatingChanged(content, it) }
                )
            }
        }
    }
}

@Composable
fun CurrentContentCard(
    content: TrackedContent,
    onClick: () -> Unit,
    onMarkFinished: () -> Unit,
    onStatusChanged: (ContentStatus) -> Unit,
    onRatingChanged: (Int?) -> Unit
) {
    Card(
        modifier = Modifier
            .width(336.dp)
            .animateContentSize()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = typeColor(content.type).copy(alpha = 0.16f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!content.coverUrl.isNullOrBlank()) {
                CoverArt(content = content, modifier = Modifier.size(width = 58.dp, height = 78.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(content.title, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(content.type.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                InlineRatingSelector(content.rating, onRatingChanged, maxStars = 5)
                QuickStatusRow(content.status, onStatusChanged)
            }
            FinishIconButton(onClick = onMarkFinished)
        }
    }
}

@Composable
fun HomeSummaryHeader(summary: HomeSummary) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Tu biblioteca",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            SummaryMetric(
                label = "En progreso",
                value = "${summary.inProgress}",
                modifier = Modifier.weight(1f),
                container = Color(0xFFE4F4ED)
            )
            SummaryMetric(
                label = "Este mes",
                value = "${summary.finishedThisMonth}",
                modifier = Modifier.weight(1f),
                container = Color(0xFFFFE5DF)
            )
            SummaryMetric(
                label = "Pendientes",
                value = "${summary.pending}",
                modifier = Modifier.weight(1f),
                container = Color(0xFFE8E1FF)
            )
        }
    }
}

@Composable
fun SummaryMetric(label: String, value: String, modifier: Modifier = Modifier, container: Color) {
    Column(
        modifier = modifier
            .background(container, RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun TypeTabs(
    selected: ContentType?,
    onSelected: (ContentType?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selected == null,
            onClick = { onSelected(null) },
            label = { Text("Todo") }
        )
        ContentType.entries.forEach { value ->
            FilterChip(
                selected = selected == value,
                onClick = { onSelected(value) },
                label = { Text(value.label) }
            )
        }
    }
}

@Composable
fun ContentCard(
    content: TrackedContent,
    onClick: () -> Unit,
    onMarkFinished: () -> Unit,
    onStatusChanged: (ContentStatus) -> Unit,
    onRatingChanged: (Int?) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = typeColor(content.type).copy(alpha = 0.14f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!content.coverUrl.isNullOrBlank()) {
                CoverArt(content = content, modifier = Modifier.size(width = 58.dp, height = 82.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = content.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = listOfNotNull(content.type.label, content.genre).joinToString(" - "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    InlineRatingSelector(content.rating, onRatingChanged, maxStars = 5)
                    Text(
                        text = content.rating?.let { "$it/10" } ?: "Sin nota",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                QuickStatusRow(content.status, onStatusChanged)
            }
            if (content.status != ContentStatus.FINISHED) {
                FinishIconButton(onClick = onMarkFinished)
            }
        }
    }
}

@Composable
fun QuickStatusRow(selected: ContentStatus, onSelected: (ContentStatus) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        listOf(ContentStatus.PENDING, ContentStatus.IN_PROGRESS, ContentStatus.FINISHED).forEach { status ->
            StatusMiniButton(
                status = status,
                selected = selected == status,
                onClick = { onSelected(status) }
            )
        }
    }
}

@Composable
fun StatusMiniButton(status: ContentStatus, selected: Boolean, onClick: () -> Unit) {
    val container = if (selected) statusColor(status) else MaterialTheme.colorScheme.surface.copy(alpha = 0.78f)
    val textColor = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = Modifier
            .height(30.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(container)
            .padding(horizontal = 8.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(status.shortLabel(), style = MaterialTheme.typography.labelSmall, color = textColor, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun InlineRatingSelector(rating: Int?, onSelected: (Int?) -> Unit, maxStars: Int = 10) {
    Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
        (1..maxStars).forEach { value ->
            val mappedValue = if (maxStars == 5) value * 2 else value
            Text(
                text = if ((rating ?: 0) >= mappedValue) "\u2605" else "\u2606",
                color = if ((rating ?: 0) >= mappedValue) Color(0xFFFFB4A2) else MaterialTheme.colorScheme.outline.copy(alpha = 0.58f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onSelected(mappedValue) }
            )
        }
    }
}

fun ContentStatus.shortLabel(): String {
    return when (this) {
        ContentStatus.PENDING -> "Pend."
        ContentStatus.IN_PROGRESS -> "Progreso"
        ContentStatus.FINISHED -> "Fin."
        ContentStatus.ABANDONED -> "Abandonado"
    }
}

fun statusColor(status: ContentStatus): Color {
    return when (status) {
        ContentStatus.PENDING -> Color(0xFFFFE5DF)
        ContentStatus.IN_PROGRESS -> Color(0xFFE8E1FF)
        ContentStatus.FINISHED -> Color(0xFFE4F4ED)
        ContentStatus.ABANDONED -> Color(0xFFF0E7F2)
    }
}

fun ContentType.singleLabel(): String {
    return when (this) {
        ContentType.SERIES -> "Serie"
        ContentType.ANIME -> "Anime"
        ContentType.MOVIE -> "Película"
        ContentType.BOOK -> "Libro"
    }
}

