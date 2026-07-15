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
fun HomeScreen(
    state: HomeUiState,
    onTypeSelected: (ContentType?) -> Unit,
    onStatusSelected: (ContentStatus?) -> Unit,
    onSearchChanged: (String) -> Unit,
    onAdd: () -> Unit,
    onMarkFinished: (TrackedContent) -> Unit,
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
                    text = { Text(selectedType.label) }
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
                        onMarkFinished = onMarkFinished
                    )
                }
            }
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
                TypeTabs(
                    selected = state.selectedType,
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
                    ContentCard(
                        content = content,
                        onClick = { onOpen(content.id) },
                        onMarkFinished = { onMarkFinished(content) }
                    )
                }
            }
        }
    }
}

@Composable
fun CurrentSection(
    contents: List<TrackedContent>,
    onOpen: (Long) -> Unit,
    onMarkFinished: (TrackedContent) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Ahora en progreso",
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
                    onMarkFinished = { onMarkFinished(content) }
                )
            }
        }
    }
}

@Composable
fun CurrentContentCard(content: TrackedContent, onClick: () -> Unit, onMarkFinished: () -> Unit) {
    Card(
        modifier = Modifier
            .width(220.dp)
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
                CoverArt(content = content, modifier = Modifier.size(width = 54.dp, height = 72.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(content.title, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(content.type.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                RatingStars(content.rating, compact = true)
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
fun ContentCard(content: TrackedContent, onClick: () -> Unit, onMarkFinished: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = typeColor(content.type).copy(alpha = 0.14f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!content.coverUrl.isNullOrBlank()) {
                    CoverArt(content = content, modifier = Modifier.size(width = 54.dp, height = 72.dp))
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = content.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
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
                }
                RatingPill(content.rating)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusPill(content.status)
                if (content.status != ContentStatus.FINISHED) {
                    FinishIconButton(onClick = onMarkFinished)
                }
            }
        }
    }
}

