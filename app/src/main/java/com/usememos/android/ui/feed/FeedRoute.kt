package com.usememos.android.ui.feed

import android.Manifest
import android.content.Context
import android.location.LocationManager
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import com.usememos.android.data.local.SyncState
import com.usememos.android.ui.theme.Accent
import com.usememos.android.domain.model.Memo
import com.usememos.android.domain.model.MemoTag
import com.usememos.android.domain.util.extractTags
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedRoute(
    state: FeedUiState,
    onContentChanged: (String) -> Unit,
    onVisibilitySelected: (String) -> Unit,
    onTagSelected: (String?) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onDateSelected: (LocalDate?) -> Unit,
    onMonthChanged: (YearMonth) -> Unit,
    onSaveClicked: () -> Unit,
    onRetrySync: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var sidebarOpen by remember { mutableStateOf(false) }
    var memoLinkPickerOpen by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { selectedUri ->
            val addition = buildUploadMarkdown(selectedUri)
            val updated = appendToDraft(state.draftContent, addition)
            onContentChanged(updated)
        }
    }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            getLastKnownLocationMarkdown(context)?.let { markdown ->
                onContentChanged(appendToDraft(state.draftContent, markdown))
            }
        }
    }

    LaunchedEffect(state.infoMessage) {
        state.infoMessage?.let { snackbarHostState.showSnackbar(it) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.surface,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    TopHeader(
                        onMenu = { sidebarOpen = true },
                        onRefresh = onRetrySync,
                        onOpenSettings = onOpenSettings,
                    )
                }

                item {
                    QuickCaptureCard(
                        draftContent = state.draftContent,
                        selectedVisibility = state.selectedVisibility,
                        isSaving = state.isSaving,
                        memos = state.memos,
                        onContentChanged = onContentChanged,
                        onVisibilitySelected = onVisibilitySelected,
                        onUpload = { filePicker.launch("*/*") },
                        onLinkMemo = { memoLinkPickerOpen = true },
                        onLocation = {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                getLastKnownLocationMarkdown(context)?.let { markdown ->
                                    onContentChanged(appendToDraft(state.draftContent, markdown))
                                }
                            } else {
                                locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                            }
                        },
                        onSaveClicked = onSaveClicked,
                    )
                }

                if (state.memos.isEmpty()) {
                    item { EmptyStateCard(isFiltered = state.selectedTag != null, isConfigured = state.isConfigured) }
                } else {
                    items(state.memos, key = Memo::id) { memo ->
                        MemoCard(memo = memo)
                    }
                }
            }
        }

        if (sidebarOpen) {
            SidebarOverlay(
                availableTags = state.availableTags,
                selectedTag = state.selectedTag,
                searchQuery = state.searchQuery,
                selectedDate = state.selectedDate,
                memoDates = state.memoDates,
                currentMonth = state.currentMonth,
                onSearchQueryChanged = onSearchQueryChanged,
                onTagSelected = onTagSelected,
                onDateSelected = onDateSelected,
                onMonthChanged = onMonthChanged,
                onClose = { sidebarOpen = false },
            )
        }

        if (memoLinkPickerOpen) {
            MemoLinkPicker(
                memos = state.memos,
                onSelect = { memo ->
                    val title = memo.content.lineSequence().firstOrNull()?.takeIf { it.isNotBlank() } ?: "Memo"
                    val markdown = "[$title](memos://${memo.id})"
                    onContentChanged(appendToDraft(state.draftContent, markdown))
                    memoLinkPickerOpen = false
                },
                onDismiss = { memoLinkPickerOpen = false },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TopHeader(
    onMenu: () -> Unit,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(id = com.usememos.android.R.drawable.logo),
                contentDescription = "UseMemos",
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
            Text("Memos", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
        }
        Row {
            IconButton(onClick = onRefresh) {
                Icon(Icons.Outlined.Refresh, contentDescription = "Sync", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Outlined.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onMenu) {
                Icon(Icons.Outlined.Menu, contentDescription = "Menu", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickCaptureCard(
    draftContent: String,
    selectedVisibility: String,
    isSaving: Boolean,
    memos: List<Memo>,
    onContentChanged: (String) -> Unit,
    onVisibilitySelected: (String) -> Unit,
    onUpload: () -> Unit,
    onLinkMemo: () -> Unit,
    onLocation: () -> Unit,
    onSaveClicked: () -> Unit,
) {
    var addMenuOpen by remember { mutableStateOf(false) }
    var visibilityMenuOpen by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = draftContent,
                onValueChange = onContentChanged,
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                placeholder = { Text("Any thoughts...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceContainer,
                    focusedBorderColor = MaterialTheme.colorScheme.surfaceContainer,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.clickable { addMenuOpen = true },
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Icon(
                        Icons.Outlined.Add,
                        contentDescription = "Add",
                        modifier = Modifier.padding(8.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                DropdownMenu(
                    expanded = addMenuOpen,
                    onDismissRequest = { addMenuOpen = false },
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    listOf("Upload", "Link Memo", "Location").forEach { label ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                addMenuOpen = false
                                when (label) {
                                    "Upload" -> onUpload()
                                    "Link Memo" -> if (memos.isNotEmpty()) onLinkMemo()
                                    "Location" -> onLocation()
                                }
                            },
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box {
                        Surface(
                            onClick = { visibilityMenuOpen = true },
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(10.dp),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    selectedVisibility.lowercase().replaceFirstChar(Char::uppercase),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Icon(
                                    Icons.Outlined.KeyboardArrowDown,
                                    contentDescription = "Visibility",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                        DropdownMenu(
                            expanded = visibilityMenuOpen,
                            onDismissRequest = { visibilityMenuOpen = false },
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        ) {
                            listOf("PRIVATE", "PROTECTED", "PUBLIC").forEach { visibility ->
                                DropdownMenuItem(
                                    text = { Text(visibility.lowercase().replaceFirstChar(Char::uppercase)) },
                                    onClick = {
                                        visibilityMenuOpen = false
                                        onVisibilitySelected(visibility)
                                    },
                                )
                            }
                        }
                    }
                    Surface(
                        modifier = Modifier.clickable(enabled = !isSaving, onClick = onSaveClicked),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(18.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                )
                            } else {
                                Text("Save", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MemoCard(memo: Memo) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
            Text(
                text = memo.displayTime.atZone(ZoneId.systemDefault()).format(cardDateFormatter),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SyncBadge(memo.syncState)
            }
            MarkdownMemoText(
                markdown = memo.content,
                modifier = Modifier.fillMaxWidth(),
            )
            MemoTagRow(memo = memo)
            memo.errorMessage?.takeIf { it.isNotBlank() }?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MemoTagRow(memo: Memo) {
    val tags = remember(memo.content) { extractTags(memo.content) }
    if (tags.isEmpty()) return

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        tags.forEach { tag ->
            Text(
                text = tag,
                style = MaterialTheme.typography.bodyMedium,
                color = Accent,
            )
        }
    }
}

@Composable
private fun SyncBadge(syncState: SyncState) {
    val background = when (syncState) {
        SyncState.PENDING -> MaterialTheme.colorScheme.primaryContainer
        SyncState.SYNCED -> MaterialTheme.colorScheme.surfaceContainerHigh
        SyncState.FAILED -> MaterialTheme.colorScheme.errorContainer
    }
    val label = when (syncState) {
        SyncState.PENDING -> "Queued"
        SyncState.SYNCED -> "Synced"
        SyncState.FAILED -> "Retry"
    }

    Box(
        modifier = Modifier
            .background(background, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SidebarOverlay(
    availableTags: List<MemoTag>,
    selectedTag: String?,
    searchQuery: String,
    selectedDate: LocalDate?,
    memoDates: Set<LocalDate>,
    currentMonth: YearMonth,
    onSearchQueryChanged: (String) -> Unit,
    onTagSelected: (String?) -> Unit,
    onDateSelected: (LocalDate?) -> Unit,
    onMonthChanged: (YearMonth) -> Unit,
    onClose: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.End,
    ) {
        Box(
            modifier = Modifier
                .weight(0.18f)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.55f))
                .clickable(onClick = onClose),
        )
        Surface(
            modifier = Modifier
                .weight(0.82f)
                .fillMaxHeight(),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChanged,
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        placeholder = { Text("Search memos...") },
                        leadingIcon = {
                            Icon(Icons.Outlined.Search, contentDescription = null)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        ),
                    )
                    IconButton(onClick = onClose) {
                        Icon(Icons.Outlined.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                CalendarPanel(
                    currentMonth = currentMonth,
                    selectedDate = selectedDate,
                    memoDates = memoDates,
                    onMonthChanged = onMonthChanged,
                    onDateSelected = onDateSelected,
                )

                Text("Tags", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                TagCloud(
                    availableTags = availableTags,
                    selectedTag = selectedTag,
                    onTagSelected = onTagSelected,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CalendarPanel(
    currentMonth: YearMonth,
    selectedDate: LocalDate?,
    memoDates: Set<LocalDate>,
    onMonthChanged: (YearMonth) -> Unit,
    onDateSelected: (LocalDate?) -> Unit,
) {
    val firstDay = currentMonth.atDay(1)
    val startOffset = (firstDay.dayOfWeek.value % 7)
    val dates = buildList<LocalDate?> {
        repeat(startOffset) { add(null) }
        for (day in 1..currentMonth.lengthOfMonth()) {
            add(currentMonth.atDay(day))
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { onMonthChanged(currentMonth.minusMonths(1)) }) {
                Icon(Icons.Outlined.ChevronLeft, contentDescription = "Previous month", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            IconButton(onClick = { onMonthChanged(currentMonth.plusMonths(1)) }) {
                Icon(Icons.Outlined.ChevronRight, contentDescription = "Next month", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT").forEach { day ->
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(day, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        dates.chunked(7).forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                week.forEach { date ->
                    val isSelected = selectedDate == date && date != null
                    val hasMemo = date != null && memoDates.contains(date)
                    val background = when {
                        isSelected -> MaterialTheme.colorScheme.primaryContainer
                        hasMemo -> MaterialTheme.colorScheme.surfaceContainerHigh
                        else -> MaterialTheme.colorScheme.surface
                    }
                    val borderColor = if (isSelected || hasMemo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .then(
                                if (date != null) {
                                    Modifier
                                        .border(
                                            width = if (isSelected) 1.dp else 0.5.dp,
                                            color = borderColor,
                                            shape = RoundedCornerShape(10.dp),
                                        )
                                        .background(background, RoundedCornerShape(10.dp))
                                        .clickable { onDateSelected(date) }
                                } else {
                                    Modifier
                                },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(date?.dayOfMonth?.toString().orEmpty(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagCloud(
    availableTags: List<MemoTag>,
    selectedTag: String?,
    onTagSelected: (String?) -> Unit,
) {
    if (availableTags.isEmpty()) {
        Text("No tags yet", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        return
    }

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        availableTags.forEach { tag ->
            Text(
                text = "${tag.value}(${tag.count})",
                color = if (selectedTag == tag.value) Accent else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.clickable { onTagSelected(tag.value) },
            )
        }
    }
}

@Composable
private fun EmptyStateCard(isFiltered: Boolean, isConfigured: Boolean) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
        ) {
            Text(
                when {
                    !isConfigured -> "Connect your server to load memos"
                    isFiltered -> "No memos match this tag"
                    else -> "No memos yet"
                },
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = when {
                    !isConfigured -> "Open settings, add your Memos URL and token, then sync."
                    isFiltered -> "Try another tag from the sidebar."
                    else -> "Create a memo above and it will show up here."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MemoLinkPicker(
    memos: List<Memo>,
    onSelect: (Memo) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Link Memo") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(memos.take(20), key = Memo::id) { memo ->
                    Text(
                        text = memo.content.lineSequence().firstOrNull()?.takeIf { it.isNotBlank() } ?: "Untitled memo",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(memo) }
                            .padding(vertical = 8.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
    )
}

private fun appendToDraft(current: String, addition: String): String {
    if (addition.isBlank()) return current
    return if (current.isBlank()) addition else "$current\n$addition"
}

private fun buildUploadMarkdown(uri: Uri): String {
    val value = uri.toString()
    val isImage = value.endsWith(".png", true) || value.endsWith(".jpg", true) ||
        value.endsWith(".jpeg", true) || value.endsWith(".gif", true) || value.endsWith(".webp", true)
    return if (isImage) {
        "![]($value)"
    } else {
        "[Attachment]($value)"
    }
}

private fun getLastKnownLocationMarkdown(context: Context): String? {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
    val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
    val location = providers.firstNotNullOfOrNull { provider ->
        runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull()
    } ?: return null
    return "[Location](geo:${location.latitude},${location.longitude}?q=${location.latitude},${location.longitude})"
}

private val cardDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, MMM d")
