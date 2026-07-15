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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale


@Composable
fun CoverArt(content: TrackedContent, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(typeColor(content.type).copy(alpha = 0.22f), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = content.coverUrl,
            contentDescription = "Portada de ${content.title}",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
fun FinishIconButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .background(Color(0xFFE4F4ED), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "\u2713",
            color = Color(0xFF24675B),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun StatusPill(status: ContentStatus) {
    val container = when (status) {
        ContentStatus.PENDING -> Color(0xFFFFE5DF)
        ContentStatus.IN_PROGRESS -> Color(0xFFE8E1FF)
        ContentStatus.FINISHED -> Color(0xFFE4F4ED)
        ContentStatus.ABANDONED -> Color(0xFFF0E7F2)
    }
    Box(
        modifier = Modifier
            .background(container, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(status.label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
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
            text = typeIcon(type),
            color = textColor,
            fontWeight = FontWeight.Bold
        )
    }
}

fun typeIcon(type: ContentType): String {
    return when (type) {
        ContentType.SERIES -> "TV"
        ContentType.ANIME -> "*"
        ContentType.MOVIE -> "\u25B6"
        ContentType.BOOK -> "\u25A4"
    }
}

@Composable
fun RatingText(rating: Int?) {
    Text(
        text = rating?.let { "$it/10" } ?: "-",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold
    )
}

@Composable
fun RatingPill(rating: Int?) {
    Box(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.78f), RoundedCornerShape(8.dp))
            .padding(horizontal = 9.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        RatingStars(rating = rating, compact = true)
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
        Text("Agregá una serie, película, anime o libro para empezar.")
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
fun RatingStars(rating: Int?, compact: Boolean = false) {
    val value = rating ?: 0
    Row(horizontalArrangement = Arrangement.spacedBy(if (compact) 0.dp else 2.dp)) {
        (1..10).forEach { index ->
            Text(
                text = if (value >= index) "\u2605" else "\u2606",
                color = if (value >= index) Color(0xFFFFB4A2) else MaterialTheme.colorScheme.outline.copy(alpha = 0.72f),
                fontSize = if (compact) 10.sp else 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun BackChevron(onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text("<", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
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

