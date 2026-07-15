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
fun EditContentScreen(
    state: EditUiState,
    onTitleChanged: (String) -> Unit,
    onTypeChanged: (ContentType) -> Unit,
    onStatusChanged: (ContentStatus) -> Unit,
    onRatingChanged: (Int?) -> Unit,
    onGenreChanged: (String) -> Unit,
    onCoverUrlChanged: (String) -> Unit,
    onSearchCover: () -> Unit,
    onCoverSelected: (CoverSearchResult) -> Unit,
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
                GenreSelector(
                    type = state.type,
                    selectedGenres = state.genre,
                    onGenreChanged = onGenreChanged
                )
            }
            item {
                CoverPicker(
                    state = state,
                    onCoverUrlChanged = onCoverUrlChanged,
                    onSearchCover = onSearchCover,
                    onCoverSelected = onCoverSelected
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
                Button(
                    onClick = onSave,
                    enabled = !state.isSaving,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (state.isSaving) "Guardando..." else "Guardar")
                }
            }
        }
    }
}

@Composable
fun CoverPicker(
    state: EditUiState,
    onCoverUrlChanged: (String) -> Unit,
    onSearchCover: () -> Unit,
    onCoverSelected: (CoverSearchResult) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Portada", fontWeight = FontWeight.SemiBold)
        if (state.coverUrl.isNotBlank()) {
            AsyncImage(
                model = state.coverUrl,
                contentDescription = "Portada seleccionada",
                modifier = Modifier
                    .size(width = 86.dp, height = 118.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
        }
        OutlinedTextField(
            value = state.coverUrl,
            onValueChange = onCoverUrlChanged,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("URL de portada") },
            placeholder = { Text("https://...") }
        )
        OutlinedButton(
            onClick = onSearchCover,
            enabled = !state.isSearchingCover,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (state.isSearchingCover) "Buscando..." else "Buscar portada automaticamente")
        }
        state.coverSearchError?.let { error ->
            Text(error, color = MaterialTheme.colorScheme.error)
        }
        if (state.coverResults.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Elegir portada", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                state.coverResults.forEach { result ->
                    CoverResultRow(result = result, onClick = { onCoverSelected(result) })
                }
            }
        }
    }
}

@Composable
fun CoverResultRow(result: CoverSearchResult, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = result.imageUrl,
                contentDescription = result.title,
                modifier = Modifier
                    .size(width = 48.dp, height = 68.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(result.title, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (result.subtitle.isNotBlank()) {
                    Text(result.subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
            }
            Text("Usar", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun GenreSelector(
    type: ContentType,
    selectedGenres: String,
    onGenreChanged: (String) -> Unit
) {
    val genres = genresFor(type)
    val selected = selectedGenreList(selectedGenres)

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Generos", fontWeight = FontWeight.SemiBold)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            genres.forEach { genre ->
                val isSelected = genre in selected
                FilterChip(
                    selected = isSelected,
                    onClick = { onGenreChanged(toggleGenre(selected, genre)) },
                    label = { Text(genre) }
                )
            }
        }
    }
}

fun selectedGenreList(value: String): List<String> {
    return value.split(",")
        .map { it.trim() }
        .filter { it.isNotBlank() }
}

fun toggleGenre(selected: List<String>, genre: String): String {
    val next = if (genre in selected) selected - genre else selected + genre
    return next.joinToString(", ")
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
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 8.dp)
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            (1..10).forEach { value ->
                Text(
                    text = if ((rating ?: 0) >= value) "\u2605" else "\u2606",
                    color = if ((rating ?: 0) >= value) Color(0xFFFFB4A2) else MaterialTheme.colorScheme.outline,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onSelected(value) }
                )
            }
        }
        TextButton(onClick = { onSelected(null) }) {
            Text(if (rating == null) "Sin nota seleccionada" else "Quitar nota")
        }
    }
}

