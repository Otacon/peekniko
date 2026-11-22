package peekniko.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import peekniko.NetworkMethod
import peekniko.ui.networkcalls.CallStatusFilter
import peekniko.ui.networkcalls.PeeknikoViewModel
import peekniko.ui.networkcalls.State

@Composable
internal fun CallListScreen(
    viewModel: PeeknikoViewModel,
    onClose: () -> Unit,
    selectedCallId: String?,
    onCallSelected: (String) -> Unit,
) {
    val state by viewModel.state.collectAsState()
    CallListContent(state, viewModel, selectedCallId, onClose, onCallSelected)
}

@Composable
private fun CallListContent(
    state: State,
    viewModel: PeeknikoViewModel,
    selectedCallId: String?,
    onClose: () -> Unit,
    onCallSelected: (String) -> Unit,
) {
    var searchExpanded by remember { mutableStateOf(state.query.isNotEmpty()) }
    var showClearConfirmation by remember { mutableStateOf(false) }
    val searchFocus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    fun collapseSearch() {
        viewModel.onSearchQueryChanged("")
        searchExpanded = false
        focusManager.clearFocus()
    }
    LaunchedEffect(searchExpanded) {
        if (searchExpanded) searchFocus.requestFocus()
    }
    val listState = rememberLazyListState()
    val firstCallId = state.calls.firstOrNull()?.id
    val lastFirstCallId = remember { object { var value: String? = null } }
    SideEffect {
        val previous = lastFirstCallId.value
        lastFirstCallId.value = firstCallId
        if (firstCallId != previous && listState.firstVisibleItemIndex == 0) {
            listState.requestScrollToItem(0)
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (searchExpanded) {
                        TextField(
                            value = state.query,
                            onValueChange = viewModel::onSearchQueryChanged,
                            modifier = Modifier.fillMaxWidth().focusRequester(searchFocus)
                                .onPreviewKeyEvent {
                                    if (it.key == Key.Escape && it.type == KeyEventType.KeyDown) {
                                        collapseSearch()
                                        true
                                    } else false
                                },
                            placeholder = { Text("Search Endpoint") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                            trailingIcon = {
                                if (state.query.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                        Icon(Icons.Close, contentDescription = "Clear search")
                                    }
                                }
                            },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            ),
                        )
                    } else Text("Peekniko")
                },
                navigationIcon = {
                    IconButton(onClick = { if (searchExpanded) collapseSearch() else onClose() }) {
                        Icon(
                            imageVector = if (searchExpanded) Icons.ArrowBack else Icons.Close,
                            contentDescription = if (searchExpanded) "Collapse search" else "Close",
                        )
                    }
                },
                actions = {
                    if (!searchExpanded) {
                        IconButton(onClick = { searchExpanded = true }) {
                            Icon(Icons.Search, contentDescription = "Search Endpoint")
                        }
                        CallListFilterMenu(viewModel, state)
                        if (state.showClearCalls) {
                            IconButton(onClick = { showClearConfirmation = true }) {
                                Icon(Icons.Delete, contentDescription = "Clear all calls")
                            }
                        }
                    }
                },
            )
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)

        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (state.showFilteredEmptyState) {
                    Text(
                        text = "No calls match your filters. Change or clear the filters to see captured calls.",
                        modifier = Modifier.align(Alignment.Center).padding(24.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize(), state = listState) {
                        itemsIndexed(state.calls, key = { _, item -> item.id }) { index, item ->
                            CallListItem(
                                item = item,
                                selected = item.id == selectedCallId,
                                onClick = { onCallSelected(item.id) },
                            )
                            if (index < state.calls.lastIndex) {
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }
    }
    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearConfirmation = false },
            title = { Text("Clear all network calls?") },
            text = { Text("All captured network calls will be deleted. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onClearClick()
                    showClearConfirmation = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmation = false }) { Text("Cancel") }
            },
        )
    }
}

private enum class FilterMenuPage { ROOT, METHOD, STATUS }

@Composable
private fun CallListFilterMenu(viewModel: PeeknikoViewModel, state: State) {
    var page by remember { mutableStateOf<FilterMenuPage?>(null) }
    Box {
        IconButton(onClick = { page = FilterMenuPage.ROOT }) {
            Icon(
                imageVector = if (state.hasActiveFilters) Icons.FilterApplied else Icons.Filter,
                contentDescription = if (state.hasActiveFilters) "Filters applied" else "Filters",
            )
        }
        DropdownMenu(expanded = page != null, onDismissRequest = { page = null }) {
            when (page) {
                FilterMenuPage.ROOT -> {
                    DropdownMenuItem(
                        text = { Text("Method") },
                        onClick = { page = FilterMenuPage.METHOD },
                    )
                    DropdownMenuItem(
                        text = { Text("Status") },
                        onClick = { page = FilterMenuPage.STATUS },
                    )
                    if (state.showResetFilters) {
                        DropdownMenuItem(text = { Text("Clear Filters") }, onClick = {
                            viewModel.onClearFiltersClick()
                            page = null
                        })
                    }
                }

                FilterMenuPage.METHOD, FilterMenuPage.STATUS -> {
                    val isMethod = page == FilterMenuPage.METHOD
                    if (isMethod) {
                        FilterOptions(NetworkMethod.entries.filter { it != NetworkMethod.UNKNOWN }
                            .map { it to it.name }) {
                            viewModel.onMethodFilterSelected(it)
                            page = null
                        }
                    } else {
                        FilterOptions(CallStatusFilter.entries.map { it to it.label() }) {
                            viewModel.onStatusFilterSelected(it)
                            page = null
                        }
                    }
                }

                null -> Unit
            }
        }
    }
}

@Composable
private fun <T> FilterOptions(options: List<Pair<T, String>>, onSelected: (T?) -> Unit) {
    DropdownMenuItem(text = { Text("All") }, onClick = { onSelected(null) })
    options.forEach { (value, title) ->
        DropdownMenuItem(text = { Text(title) }, onClick = { onSelected(value) })
    }
}

private fun CallStatusFilter.label(): String = when (this) {
    CallStatusFilter.SUCCESS_2XX -> "Success (2xx)"
    CallStatusFilter.NON_SUCCESS -> "Non-success (non-2xx)"
    CallStatusFilter.EXCEPTION -> "Request exceptions"
}

@Composable
private fun CallListItem(
    item: PeeknikoCallRowItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        modifier = modifier.clickable(onClick = onClick),
        colors = ListItemDefaults.colors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
        headlineContent = {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                CallBadge(modifier = Modifier.width(72.dp), text = item.method)
                Spacer(Modifier.width(8.dp))
                Text(item.urlPath)
            }

        },
        supportingContent = { Text(item.subtitle) },
        trailingContent = {
            CallBadge(
                text = item.statusBadge,
                containerColor = item.statusChipColors(),
                contentColor = item.statusChipContentColor(),
            )
        },
    )
}

@Composable
private fun CallBadge(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.onSurface,
    contentColor: Color = MaterialTheme.colorScheme.surface,
) {
    Surface(
        modifier = modifier,
        shape = AssistChipDefaults.shape,
        color = containerColor,
        contentColor = contentColor,
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(text, maxLines = 1, softWrap = false)
        }
    }
}

@Composable
private fun PeeknikoCallRowItem.statusChipColors(): Color = when {
    exceptionMessage != null -> StatusError
    responseCode == null -> MaterialTheme.colorScheme.onSurface
    responseCode in 200..299 -> StatusSuccess
    else -> StatusNonSuccess
}

@Composable
private fun PeeknikoCallRowItem.statusChipContentColor(): Color = when {
    exceptionMessage == null && responseCode == null -> MaterialTheme.colorScheme.surface
    else -> OnStatus
}
