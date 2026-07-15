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
fun DetailScreen(
    state: DetailUiState,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onDeleted: () -> Unit,
    onDelete: () -> Unit,
    onStatusChanged: (ContentStatus) -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.deleted) {
        if (state.deleted) onDeleted()
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminar contenido") },
            text = { Text("Esta accion no se puede deshacer. Â¿Seguro que queres eliminarlo?") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDelete()
                }) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
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
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    DetailHeader(content)
                }
                item {
                    DetailQuickActions(
                        content = content,
                        onEdit = { onEdit(content.id) },
                        onStatusChanged = onStatusChanged
                    )
                }
                item {
                    DetailSection(title = "Datos") {
                        InfoLine("Tipo", content.type.label)
                        InfoLine("Generos", content.genre ?: "-")
                        InfoLine("Estado", content.status.label)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Calificacion", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (content.rating == null) {
                                Text("Sin calificar", fontWeight = FontWeight.SemiBold)
                            } else {
                                RatingStars(content.rating)
                            }
                        }
                    }
                }
                item {
                    DetailSection(title = "Fechas") {
                        InfoLine("Agregado", content.createdAt.toLocalDate().toString())
                        InfoLine("Inicio", content.startDate?.toString() ?: "-")
                        InfoLine("Finalizacion", content.finishedDate?.toString() ?: "-")
                    }
                }
                item {
                    DetailSection(title = "Notas") {
                        Text(
                            text = content.notes ?: "Sin notas",
                            color = if (content.notes == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                item {
                    OutlinedButton(onClick = { showDeleteDialog = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Eliminar")
                    }
                }
            }
        }
    }
}

@Composable
fun DetailHeader(content: TrackedContent) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(typeColor(content.type).copy(alpha = 0.22f), RoundedCornerShape(8.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (!content.coverUrl.isNullOrBlank()) {
                CoverArt(content = content, modifier = Modifier.size(width = 86.dp, height = 118.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    content.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    listOfNotNull(content.type.label, content.genre).joinToString(" - "),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            StatusPill(content.status)
            RatingStars(content.rating)
        }
    }
}

@Composable
fun DetailQuickActions(
    content: TrackedContent,
    onEdit: () -> Unit,
    onStatusChanged: (ContentStatus) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(onClick = onEdit) {
            Text("Editar")
        }
        if (content.status != ContentStatus.FINISHED) {
            OutlinedButton(onClick = { onStatusChanged(ContentStatus.FINISHED) }) {
                Text("Terminar")
            }
        }
        if (content.status != ContentStatus.IN_PROGRESS) {
            OutlinedButton(onClick = { onStatusChanged(ContentStatus.IN_PROGRESS) }) {
                Text("En progreso")
            }
        }
        if (content.status != ContentStatus.ABANDONED) {
            OutlinedButton(onClick = { onStatusChanged(ContentStatus.ABANDONED) }) {
                Text("Abandonar")
            }
        }
    }
}

@Composable
fun DetailSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        content()
    }
}

@Composable
fun InfoLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}

