package com.mistyislet.app.ui.places

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.DoorFront
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mistyislet.app.R
import com.mistyislet.app.domain.model.Place
import com.mistyislet.app.ui.theme.Danger
import com.mistyislet.app.ui.theme.IosBlue
import com.mistyislet.app.ui.theme.IosGreen
import com.mistyislet.app.ui.theme.IosIndigo
import com.mistyislet.app.ui.theme.IosOrange
import com.mistyislet.app.ui.theme.IosPink
import com.mistyislet.app.ui.theme.IosPurple
import com.mistyislet.app.ui.theme.IosRed
import com.mistyislet.app.ui.theme.IosTeal
import com.mistyislet.app.ui.components.MistyCard
import com.mistyislet.app.ui.components.MistyBottomNavInset
import com.mistyislet.app.ui.components.MistyEmptyState
import com.mistyislet.app.ui.components.MistySearchField

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Text(
            text = uiState.orgName ?: stringResource(R.string.nav_doors),
            style = MaterialTheme.typography.headlineLarge.copy(fontSize = 34.sp, lineHeight = 40.sp),
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 84.dp, bottom = 0.dp),
        )

        if (uiState.places.size > 1 || uiState.searchQuery.isNotEmpty()) {
            MistySearchField(
                value = uiState.searchQuery,
                onValueChange = viewModel::setSearchQuery,
                placeholder = stringResource(R.string.search_places),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 0.dp),
            )
        }

        when {
            uiState.isLoading && uiState.places.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            uiState.places.isEmpty() -> {
                MistyEmptyState(
                    icon = Icons.Default.LocationOn,
                    title = stringResource(R.string.places_no_places),
                    description = stringResource(R.string.places_no_places_desc),
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = MistyBottomNavInset),
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
    MistyCard(
        cornerRadius = 16.dp,
        borderColor = Color.Transparent,
        onClick = onClick,
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(brush = gradientBrush(place)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = place.name.firstOrNull()?.uppercase() ?: "·",
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.42f),
                )
                if (place.isLockdown) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(10.dp),
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
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, lineHeight = 22.sp),
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
                Spacer(modifier = Modifier.size(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.DoorFront,
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
        ratio >= 0.9 -> IosRed
        ratio >= 0.7 -> IosOrange
        else -> IosGreen
    }
}

private val palettes = listOf(
    listOf(IosBlue, IosPurple),
    listOf(IosTeal, IosBlue),
    listOf(IosIndigo, IosPink),
    listOf(IosGreen, IosTeal),
    listOf(IosOrange, IosRed),
    listOf(IosPurple, IosIndigo),
)

private fun gradientBrush(place: Place): Brush {
    val palette = when {
        place.name.contains("Sudirman", ignoreCase = true) -> listOf(IosPurple, IosIndigo)
        place.name.contains("Kuningan", ignoreCase = true) -> listOf(IosGreen, IosTeal)
        else -> palettes[Math.floorMod(place.id.hashCode(), palettes.size)]
    }
    return Brush.linearGradient(palette)
}
