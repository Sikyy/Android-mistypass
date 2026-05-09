package com.mistyislet.app.ui.admin

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

data class AdminListItem(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val trailing: String? = null,
    val trailingColor: Color? = null,
    val leadingIcon: ImageVector? = null,
    val leadingIconColor: Color? = null,
    val leadingInitial: String? = null,
    val leadingInitialColor: Color? = null,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminListScreen(
    title: String,
    items: List<AdminListItem>,
    isLoading: Boolean,
    emptyMessage: String,
    onBack: () -> Unit,
    onRefresh: (() -> Unit)? = null,
    isRefreshing: Boolean = false,
    onItemClick: ((AdminListItem) -> Unit)? = null,
    searchPlaceholder: String? = null,
    errorMessage: String? = null,
    headerContent: (@Composable () -> Unit)? = null,
    actions: (@Composable () -> Unit)? = null,
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val filteredItems = if (searchQuery.isBlank()) items else items.filter { item ->
        item.title.contains(searchQuery, ignoreCase = true) ||
            (item.subtitle?.contains(searchQuery, ignoreCase = true) == true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = { actions?.invoke() },
            )
        },
    ) { padding ->
        val content: @Composable () -> Unit = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                when {
                    isLoading && items.isEmpty() -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    !isLoading && items.isEmpty() && errorMessage != null -> {
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.align(Alignment.Center).padding(32.dp),
                        )
                    }
                    !isLoading && items.isEmpty() -> {
                        Text(
                            text = emptyMessage,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }
                    else -> {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            if (searchPlaceholder != null) {
                                item {
                                    OutlinedTextField(
                                        value = searchQuery,
                                        onValueChange = { searchQuery = it },
                                        modifier = Modifier.fillMaxWidth(),
                                        placeholder = {
                                            Text(
                                                text = searchPlaceholder,
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
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                            }
                            if (headerContent != null) {
                                item { headerContent() }
                            }
                            items(filteredItems, key = { it.id }) { item ->
                                AdminListRow(
                                    item = item,
                                    onClick = onItemClick?.let { { it(item) } },
                                )
                            }
                        }
                    }
                }
            }
        }

        if (onRefresh != null) {
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier.padding(padding),
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    when {
                        isLoading && items.isEmpty() -> {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                        }
                        !isLoading && items.isEmpty() && errorMessage != null -> {
                            Text(
                                text = errorMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.align(Alignment.Center).padding(32.dp),
                            )
                        }
                        !isLoading && items.isEmpty() -> {
                            Text(
                                text = emptyMessage,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.align(Alignment.Center),
                            )
                        }
                        else -> {
                            LazyColumn(
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                if (searchPlaceholder != null) {
                                    item {
                                        OutlinedTextField(
                                            value = searchQuery,
                                            onValueChange = { searchQuery = it },
                                            modifier = Modifier.fillMaxWidth(),
                                            placeholder = {
                                                Text(
                                                    text = searchPlaceholder,
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
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                    }
                                }
                                if (headerContent != null) {
                                    item { headerContent() }
                                }
                                items(filteredItems, key = { it.id }) { item ->
                                    AdminListRow(
                                        item = item,
                                        onClick = onItemClick?.let { { it(item) } },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            content()
        }
    }
}

@Composable
private fun AdminListRow(item: AdminListItem, onClick: (() -> Unit)?) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (item.leadingIcon != null) {
                Icon(
                    imageVector = item.leadingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = item.leadingIconColor ?: MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            if (item.leadingInitial != null) {
                Surface(
                    shape = CircleShape,
                    color = (item.leadingInitialColor ?: MaterialTheme.colorScheme.primary).copy(alpha = 0.15f),
                    modifier = Modifier.size(36.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = item.leadingInitial,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = item.leadingInitialColor ?: MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (item.subtitle != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (item.trailing != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = item.trailing,
                    style = MaterialTheme.typography.labelSmall,
                    color = item.trailingColor ?: MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
