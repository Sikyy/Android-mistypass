package com.mistyislet.app.ui.places

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DoorFront
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mistyislet.app.R
import com.mistyislet.app.domain.model.Place
import com.mistyislet.app.ui.theme.Danger
import com.mistyislet.app.ui.theme.Success
import com.mistyislet.app.ui.theme.Warning

@Composable
fun MyPlacesScreen(
    viewModel: MyPlacesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val visiblePlaces = if (uiState.searchQuery.isBlank()) {
        uiState.places
    } else {
        uiState.places.filter { it.name.contains(uiState.searchQuery, ignoreCase = true) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = viewModel::back) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.places_back),
                )
            }
            Text(
                text = uiState.orgName ?: stringResource(R.string.nav_doors),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
        }

        if (uiState.places.size > 1 || uiState.searchQuery.isNotEmpty()) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::setSearchQuery,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                placeholder = {
                    Text(
                        text = stringResource(R.string.search_places),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                ),
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        when {
            uiState.isLoading && uiState.places.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            uiState.places.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.size(16.dp))
                        Text(
                            text = stringResource(R.string.places_no_places),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Spacer(modifier = Modifier.size(4.dp))
                        Text(
                            text = stringResource(R.string.places_no_places_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(visiblePlaces, key = { it.id }) { place ->
                        PlaceCard(place = place, onClick = { viewModel.select(place) })
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaceCard(place: Place, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(brush = gradientBrush(place.id)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = place.name.firstOrNull()?.uppercase() ?: "·",
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.45f),
                )
                if (place.isLockdown) {
                    Surface(
                        modifier = Modifier
                            .padding(10.dp)
                            .align(Alignment.TopStart),
                        shape = RoundedCornerShape(50),
                        color = Danger,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = Color.White,
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.doors_lockdown),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                            )
                        }
                    }
                }
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = place.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
                place.address?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
                Spacer(modifier = Modifier.size(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DoorFront,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${place.doorCount}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    val capacity = place.capacity
                    if (capacity != null && capacity > 0) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = occupancyColor(place),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${place.currentOccupancy ?: 0}/$capacity",
                            style = MaterialTheme.typography.labelMedium,
                            color = occupancyColor(place),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun occupancyColor(place: Place): Color {
    val capacity = place.capacity ?: return MaterialTheme.colorScheme.onSurfaceVariant
    if (capacity <= 0) return MaterialTheme.colorScheme.onSurfaceVariant
    val ratio = (place.currentOccupancy ?: 0).toDouble() / capacity.toDouble()
    return when {
        ratio >= 0.9 -> Danger
        ratio >= 0.7 -> Warning
        else -> Success
    }
}

private val palettes = listOf(
    listOf(Color(0xFF2196F3), Color(0xFF9C27B0)),
    listOf(Color(0xFF009688), Color(0xFF2196F3)),
    listOf(Color(0xFF3F51B5), Color(0xFFE91E63)),
    listOf(Color(0xFF4CAF50), Color(0xFF009688)),
    listOf(Color(0xFFFF9800), Color(0xFFF44336)),
    listOf(Color(0xFF9C27B0), Color(0xFF3F51B5)),
)

private fun gradientBrush(id: String): Brush {
    val palette = palettes[(id.hashCode().let { if (it < 0) -it else it }) % palettes.size]
    return Brush.linearGradient(palette)
}
