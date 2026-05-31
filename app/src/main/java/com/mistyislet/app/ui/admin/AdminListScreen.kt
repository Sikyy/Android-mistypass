package com.mistyislet.app.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mistyislet.app.ui.components.MistyCard
import com.mistyislet.app.ui.components.MistyBottomNavInset
import com.mistyislet.app.ui.components.MistyEmptyState
import com.mistyislet.app.ui.components.MistyNavigationTopBar
import com.mistyislet.app.ui.components.MistySearchField
import com.mistyislet.app.ui.components.MistySectionTitle
import com.mistyislet.app.ui.theme.IosGray

data class AdminListItem(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val trailing: String? = null,
    val trailingColor: Color? = null,
    val trailingChip: Boolean = false,
    val leadingIcon: ImageVector? = null,
    val leadingIconColor: Color? = null,
    val leadingInitial: String? = null,
    val leadingInitialColor: Color? = null,
    val leadingDotColor: Color? = null,
)

private val AdminListGroupedPadding = PaddingValues(
    start = 16.dp,
    top = 28.dp,
    end = 16.dp,
    bottom = MistyBottomNavInset,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminListScreen(
    title: String,
    items: List<AdminListItem>,
    isLoading: Boolean,
    emptyMessage: String,
    emptyDescription: String? = null,
    emptyIcon: ImageVector = Icons.Default.Search,
    onBack: () -> Unit,
    onRefresh: (() -> Unit)? = null,
    isRefreshing: Boolean = false,
    onItemClick: ((AdminListItem) -> Unit)? = null,
    onItemLongClick: ((AdminListItem) -> Unit)? = null,
    searchPlaceholder: String? = null,
    errorMessage: String? = null,
    headerContent: (@Composable () -> Unit)? = null,
    listSectionTitle: String? = null,
    actions: (@Composable () -> Unit)? = null,
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val trimmedQuery = searchQuery.trim()
    val filteredItems = if (trimmedQuery.isBlank()) {
        items
    } else {
        items.filter { it.matchesSearch(trimmedQuery) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            MistyNavigationTopBar(
                title = title,
                onBack = onBack,
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
                        AdminListUnavailableState(
                            icon = Icons.Default.Warning,
                            title = errorMessage,
                            description = emptyDescription,
                        )
                    }
                    !isLoading && items.isEmpty() -> {
                        AdminListUnavailableState(
                            icon = emptyIcon,
                            title = emptyMessage,
                            description = emptyDescription,
                        )
                    }
                    else -> {
                        LazyColumn(
                            contentPadding = AdminListGroupedPadding,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            if (!searchPlaceholder.isNullOrBlank()) {
                                item {
                                    MistySearchField(
                                        value = searchQuery,
                                        onValueChange = { searchQuery = it },
                                        placeholder = searchPlaceholder,
                                    )
                                }
                            }
                            if (headerContent != null) {
                                item { headerContent() }
                            }
                            if (filteredItems.isEmpty()) {
                                item {
                                    MistyEmptyState(
                                        icon = emptyIcon,
                                        title = emptyMessage,
                                        description = emptyDescription,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(280.dp),
                                    )
                                }
                            } else {
                                item {
                                    AdminListGroupWithTitle(
                                        title = listSectionTitle,
                                        items = filteredItems,
                                        onItemClick = onItemClick,
                                        onItemLongClick = onItemLongClick,
                                    )
                                }
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
                            AdminListUnavailableState(
                                icon = Icons.Default.Warning,
                                title = errorMessage,
                                description = emptyDescription,
                            )
                        }
                        !isLoading && items.isEmpty() -> {
                            AdminListUnavailableState(
                                icon = emptyIcon,
                                title = emptyMessage,
                                description = emptyDescription,
                            )
                        }
                        else -> {
                            LazyColumn(
                                contentPadding = AdminListGroupedPadding,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                if (!searchPlaceholder.isNullOrBlank()) {
                                    item {
                                        MistySearchField(
                                            value = searchQuery,
                                            onValueChange = { searchQuery = it },
                                            placeholder = searchPlaceholder,
                                        )
                                    }
                                }
                                if (headerContent != null) {
                                    item { headerContent() }
                                }
                                if (filteredItems.isEmpty()) {
                                    item {
                                        MistyEmptyState(
                                            icon = emptyIcon,
                                            title = emptyMessage,
                                            description = emptyDescription,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(280.dp),
                                        )
                                    }
                                } else {
                                    item {
                                        AdminListGroupWithTitle(
                                            title = listSectionTitle,
                                            items = filteredItems,
                                            onItemClick = onItemClick,
                                            onItemLongClick = onItemLongClick,
                                        )
                                    }
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

private fun AdminListItem.matchesSearch(query: String): Boolean {
    val q = query.lowercase()
    return title.lowercase().contains(q) ||
        subtitle.orEmpty().lowercase().contains(q) ||
        trailing.orEmpty().lowercase().contains(q)
}

@Composable
private fun AdminListUnavailableState(
    icon: ImageVector,
    title: String,
    description: String?,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = AdminListGroupedPadding,
    ) {
        item {
            MistyEmptyState(
                icon = icon,
                title = title,
                description = description,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
            )
        }
    }
}

@Composable
private fun AdminListGroupWithTitle(
    title: String?,
    items: List<AdminListItem>,
    onItemClick: ((AdminListItem) -> Unit)?,
    onItemLongClick: ((AdminListItem) -> Unit)?,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (!title.isNullOrBlank()) {
            MistySectionTitle(text = title, modifier = Modifier.padding(start = 14.dp))
        }
        AdminListGroup(
            items = items,
            onItemClick = onItemClick,
            onItemLongClick = onItemLongClick,
        )
    }
}

@Composable
private fun AdminListGroup(
    items: List<AdminListItem>,
    onItemClick: ((AdminListItem) -> Unit)?,
    onItemLongClick: ((AdminListItem) -> Unit)?,
) {
    MistyCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            items.forEachIndexed { index, item ->
                AdminListRow(
                    item = item,
                    onClick = onItemClick?.let { { it(item) } },
                    onLongClick = onItemLongClick?.let { { it(item) } },
                )
                if (index < items.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(
                            start = when {
                                item.leadingIcon != null -> 52.dp
                                item.leadingInitial != null -> 64.dp
                                item.leadingDotColor != null -> 40.dp
                                else -> 16.dp
                            },
                            end = 16.dp,
                        )
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AdminListRow(
    item: AdminListItem,
    onClick: (() -> Unit)?,
    onLongClick: (() -> Unit)?,
) {
    val interactionModifier = when {
        onClick != null || onLongClick != null -> Modifier.combinedClickable(
            onClick = onClick ?: {},
            onLongClick = onLongClick,
        )
        else -> Modifier
    }
    Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(interactionModifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
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
            // iOS people avatars use the brand-primary tint (teal) for both the circle fill and the
            // initial — see AdminUsersListView.userRow and the AdminCRUDViews user-detail header.
            val avatarColor = item.leadingInitialColor ?: MaterialTheme.colorScheme.primary
            Surface(
                shape = CircleShape,
                color = avatarColor.copy(alpha = 0.15f),
                modifier = Modifier.size(36.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = item.leadingInitial,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp, lineHeight = 20.sp),
                        fontWeight = FontWeight.SemiBold,
                        color = avatarColor,
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
        }
        if (item.leadingDotColor != null) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .padding(1.dp),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    shape = CircleShape,
                    color = item.leadingDotColor,
                    modifier = Modifier.size(10.dp),
                    content = {},
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
        }
        val subtitleLines = item.subtitle
            ?.split('\n')
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            .orEmpty()
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 15.sp,
                    lineHeight = 19.sp,
                    fontWeight = FontWeight.Medium,
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitleLines.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                subtitleLines.take(2).forEachIndexed { lineIndex, line ->
                    Text(
                        text = line,
                        style = if (lineIndex == 0) {
                            MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp, lineHeight = 16.sp)
                        } else {
                            MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, lineHeight = 13.sp)
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                            alpha = if (lineIndex == 0) 1f else 0.74f,
                        ),
                        maxLines = if (lineIndex == 0) 3 else 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        if (item.trailing != null) {
            Spacer(modifier = Modifier.width(8.dp))
            val trailingColor = item.trailingColor ?: MaterialTheme.colorScheme.onSurfaceVariant
            val trailingChipBackground = if (trailingColor == MaterialTheme.colorScheme.onSurface) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                trailingColor.copy(alpha = 0.15f)
            }
            Text(
                text = item.trailing,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 13.sp, lineHeight = 16.sp),
                fontWeight = if (item.trailingChip) FontWeight.SemiBold else FontWeight.Normal,
                color = trailingColor,
                modifier = if (item.trailingChip) {
                    Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(trailingChipBackground)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                } else {
                    Modifier
                },
            )
        }
        if (onClick != null) {
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = IosGray.copy(alpha = 0.65f),
            )
        }
    }
}
