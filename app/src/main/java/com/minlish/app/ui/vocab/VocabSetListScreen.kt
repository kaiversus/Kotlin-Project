package com.minlish.app.ui.vocab

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minlish.app.util.VocabFileIO
import com.minlish.app.util.VocabImportRow
import com.minlish.app.viewmodel.VocabViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VocabSetListScreen(
    vocabViewModel: VocabViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToWords: (setId: String, setName: String) -> Unit,
    onNavigateToCreate: () -> Unit
) {
    val state by vocabViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var deleteTarget by remember { mutableStateOf<String?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showCreateOptions by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        vocabViewModel.loadSets()
    }

    LaunchedEffect(state.error) {
        state.error?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            vocabViewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bộ từ vựng của tôi", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                }
            )
        },
        floatingActionButton = {
            Box {
                FloatingActionButton(onClick = { showCreateOptions = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Tạo bộ từ")
                }

                DropdownMenu(
                    expanded = showCreateOptions,
                    onDismissRequest = { showCreateOptions = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Tạo thủ công") },
                        onClick = {
                            showCreateOptions = false
                            onNavigateToCreate()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Nhập hàng loạt") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.CloudUpload,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        onClick = {
                            showCreateOptions = false
                            showImportDialog = true
                        }
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { vocabViewModel.updateSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Tìm kiếm bộ từ vựng...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = state.showFavoritesOnly,
                    onClick = { vocabViewModel.toggleShowFavoritesOnly(!state.showFavoritesOnly) },
                    label = { Text("Yêu thích") },
                    leadingIcon = {
                        Icon(
                            imageVector = if (state.showFavoritesOnly) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (state.showFavoritesOnly) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    shape = RoundedCornerShape(20.dp)
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    state.isLoading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    state.filteredSets.isEmpty() -> {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Không tìm thấy bộ từ vựng nào",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Nhấp vào dấu + để thêm bộ từ mới",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(state.filteredSets) { set ->
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onNavigateToWords(set.id, set.name) },
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    ),
                                    border = CardDefaults.outlinedCardBorder()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = set.name,
                                                        fontWeight = FontWeight.Bold,
                                                        style = MaterialTheme.typography.titleMedium
                                                    )

                                                    if (set.tags != "[]") {
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        val cleanTags = set.tags
                                                            .replace("[", "")
                                                            .replace("]", "")
                                                            .replace("\"", "")
                                                        SuggestionChip(
                                                            onClick = {},
                                                            label = { Text(cleanTags, fontSize = 9.sp) },
                                                            shape = RoundedCornerShape(6.dp),
                                                            modifier = Modifier.height(20.dp)
                                                        )
                                                    }
                                                }
                                                if (!set.description.isNullOrBlank()) {
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = set.description,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 2
                                                    )
                                                }
                                            }

                                            IconButton(
                                                onClick = {
                                                    vocabViewModel.toggleFavorite(set.id, !set.isFavorite)
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = if (set.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                                    contentDescription = "Yêu thích",
                                                    tint = if (set.isFavorite) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(14.dp))

                                        val total = set.totalWords
                                        val learned = set.learnedWords
                                        val progress = if (total > 0) learned.toFloat() / total else 0f
                                        val percentage = (progress * 100).toInt()

                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "$learned / $total từ đã học",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Text(
                                                    text = "$percentage%",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }

                                            LinearProgressIndicator(
                                                progress = { progress.coerceIn(0f, 1f) },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(6.dp)
                                                    .clip(RoundedCornerShape(3.dp)),
                                                color = MaterialTheme.colorScheme.primary,
                                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            IconButton(
                                                onClick = { onNavigateToWords(set.id, set.name) },
                                                modifier = Modifier
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                                    .size(36.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PlayArrow,
                                                    contentDescription = "Mở ôn tập",
                                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            IconButton(
                                                onClick = { deleteTarget = set.id },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Xoá",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    deleteTarget?.let { setId ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Xoá bộ từ?") },
            text = { Text("Hành động này sẽ xóa toàn bộ từ vựng bên trong và không thể hoàn tác.") },
            confirmButton = {
                TextButton(onClick = {
                    vocabViewModel.deleteSet(setId)
                    deleteTarget = null
                }) { Text("Xoá", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Huỷ") }
            }
        )
    }

    if (showImportDialog) {
        BulkImportDialog(
            isLoading = state.isLoading,
            onDismiss = { showImportDialog = false },
            onImport = { name, description, rows ->
                vocabViewModel.importRowsToSet(
                    setName = name,
                    setDescription = description,
                    rows = rows,
                    onDone = { showImportDialog = false }
                )
            },
            onParseFile = { uri ->
                withContext(Dispatchers.IO) {
                    VocabFileIO.parseFromUri(context, uri)
                }
            }
        )
    }
}

@Composable
private fun BulkImportDialog(
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onImport: (name: String, description: String, rows: List<VocabImportRow>) -> Unit,
    onParseFile: suspend (Uri) -> List<VocabImportRow>
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var importName by remember { mutableStateOf("") }
    var importDesc by remember { mutableStateOf("") }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var parsedRows by remember { mutableStateOf<List<VocabImportRow>?>(null) }
    var isParsing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: SecurityException) {
            // Some providers do not support persistable permission.
        }
        scope.launch {
            isParsing = true
            errorMessage = null
            try {
                val rows = onParseFile(uri)
                parsedRows = rows
                selectedFileName = context.contentResolver.query(
                    uri,
                    arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (index >= 0) cursor.getString(index) else null
                    } else null
                } ?: uri.lastPathSegment ?: "Đã chọn tệp"
                Toast.makeText(
                    context,
                    "Đã đọc ${rows.size} từ từ tệp",
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: Throwable) {
                parsedRows = null
                selectedFileName = null
                errorMessage = e.message ?: e.javaClass.simpleName ?: "Không thể đọc tệp"
            } finally {
                isParsing = false
            }
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = { Text("Nhập bộ từ vựng hàng loạt", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Hỗ trợ tệp CSV hoặc Excel (.xlsx).\nCột: Từ, Phiên âm, Nghĩa, Giải nghĩa, Ví dụ",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = importName,
                    onValueChange = { importName = it },
                    label = { Text("Tên bộ từ") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isLoading
                )

                OutlinedTextField(
                    value = importDesc,
                    onValueChange = { importDesc = it },
                    label = { Text("Mô tả (tùy chọn)") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = selectedFileName ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tệp dữ liệu") },
                        placeholder = { Text("Chưa chọn tệp") },
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading && !isParsing
                    )
                    FilledIconButton(
                        onClick = {
                            filePickerLauncher.launch(VocabFileIO.importMimeTypes())
                        },
                        enabled = !isLoading && !isParsing
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Chọn tệp")
                    }
                }

                if (isParsing) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                errorMessage?.let { message ->
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when {
                        importName.isBlank() -> errorMessage = "Vui lòng nhập tên bộ từ"
                        parsedRows == null -> errorMessage = "Vui lòng chọn tệp CSV hoặc Excel"
                        else -> {
                            errorMessage = null
                            onImport(importName, importDesc, parsedRows!!)
                        }
                    }
                },
                enabled = !isLoading && !isParsing
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Nhập dữ liệu")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("Huỷ")
            }
        }
    )
}
