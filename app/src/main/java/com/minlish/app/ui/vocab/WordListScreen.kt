package com.minlish.app.ui.vocab

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
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
import com.minlish.app.data.model.Word
import com.minlish.app.util.VocabFileFormat
import com.minlish.app.util.VocabFileIO
import com.minlish.app.viewmodel.WordItemState
import com.minlish.app.viewmodel.WordViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordListScreen(
    setId: String,
    setName: String,
    wordViewModel: WordViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToAddWord: (setId: String) -> Unit,
    onNavigateToFlashcard: (setId: String) -> Unit
) {
    val state by wordViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var deleteTarget by remember { mutableStateOf<String?>(null) }
    var editTarget by remember { mutableStateOf<Word?>(null) }
    var showExportDialog by remember { mutableStateOf(false) }
    var exportFormat by remember { mutableStateOf(VocabFileFormat.CSV) }
    var pendingExport by remember { mutableStateOf(false) }
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(setId) {
        wordViewModel.loadWords(setId)
    }

    // 1. Phân loại từ vựng theo trạng thái học tập của tab
    val allWords = state.wordItems
    val learnedWords = state.wordItems.filter { it.record != null && it.record.status != "NEW" }
    val difficultWords = state.wordItems.filter { item ->
        val record = item.record
        record != null && (
            record.status == "LEARNING" || record.status == "REVIEW"
        ) && (record.easeFactor < 2.0f || record.lastGrade < 2)
    }

    val visibleWords = when (selectedTabIndex) {
        0 -> allWords
        1 -> learnedWords
        2 -> difficultWords
        else -> allWords
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(setName, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                actions = {
                    if (state.wordItems.isNotEmpty()) {
                        IconButton(onClick = { showExportDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = "Xuất tệp",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        // Nút Ôn tập nhanh
                        IconButton(onClick = { onNavigateToFlashcard(setId) }) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Học ngay",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onNavigateToAddWord(setId) }) {
                Icon(Icons.Default.Add, contentDescription = "Thêm từ")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (state.wordItems.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Chưa có từ vựng nào", style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Nhấn + để thêm từ hoặc nhập tệp CSV",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // Ôn tập Flashcard Nút lớn
                Button(
                    onClick = { onNavigateToFlashcard(setId) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Học Flashcard (${state.wordItems.size} từ)")
                }

                // 2. Tabs phân loại trạng thái ôn tập (Sub-Tabs)
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 },
                        text = { Text("Tất cả (${allWords.size})") }
                    )
                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 },
                        text = { Text("Đã học (${learnedWords.size})") }
                    )
                    Tab(
                        selected = selectedTabIndex == 2,
                        onClick = { selectedTabIndex = 2 },
                        text = { Text("Từ khó (${difficultWords.size})") }
                    )
                }

                // 3. Danh sách từ vựng theo Tab đang chọn
                if (visibleWords.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Không có từ vựng nào trong danh mục này",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(visibleWords) { item ->
                            WordCardItem(
                                item = item,
                                onEdit = { editTarget = item.word },
                                onDelete = { deleteTarget = item.word.id }
                            )
                        }
                    }
                }
            }
        }
    }

    // Hộp thoại sửa từ vựng (Edit Word Dialog)
    editTarget?.let { word ->
        var editWordText by remember { mutableStateOf(word.word) }
        var editPronunciation by remember { mutableStateOf(word.pronunciation ?: "") }
        var editMeaning by remember { mutableStateOf(word.meaning) }
        var editDesc by remember { mutableStateOf(word.description ?: "") }
        var editExample by remember { mutableStateOf(word.exampleSentence ?: "") }
        var editCollocation by remember { mutableStateOf(word.collocation ?: "") }
        var editRelated by remember { mutableStateOf(word.relatedWords ?: "") }
        var editNote by remember { mutableStateOf(word.note ?: "") }
        var hasError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { editTarget = null },
            title = { Text("Sửa thông tin từ vựng", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = editWordText,
                        onValueChange = { editWordText = it },
                        label = { Text("Từ tiếng Anh") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editPronunciation,
                        onValueChange = { editPronunciation = it },
                        label = { Text("Phiên âm IPA") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editMeaning,
                        onValueChange = { editMeaning = it },
                        label = { Text("Nghĩa dịch") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editDesc,
                        onValueChange = { editDesc = it },
                        label = { Text("Giải thích tiếng Anh (tùy chọn)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editExample,
                        onValueChange = { editExample = it },
                        label = { Text("Ví dụ câu (tùy chọn)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editCollocation,
                        onValueChange = { editCollocation = it },
                        label = { Text("Cụm từ đi kèm (collocation)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editNote,
                        onValueChange = { editNote = it },
                        label = { Text("Ghi chú cá nhân (tùy chọn)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (hasError) {
                        Text(
                            text = "Từ tiếng Anh và Nghĩa dịch không được để trống",
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
                        if (editWordText.isBlank() || editMeaning.isBlank()) {
                            hasError = true
                        } else {
                            hasError = false
                            wordViewModel.updateWordDetails(
                                setId = setId,
                                wordId = word.id,
                                word = editWordText,
                                pronunciation = editPronunciation,
                                meaning = editMeaning,
                                description = editDesc,
                                example = editExample,
                                collocation = editCollocation,
                                relatedWords = editRelated,
                                note = editNote,
                                onDone = { editTarget = null }
                            )
                        }
                    }
                ) {
                    Text("Lưu thay đổi")
                }
            },
            dismissButton = {
                TextButton(onClick = { editTarget = null }) {
                    Text("Huỷ")
                }
            }
        )
    }

    // Hộp thoại Xóa từ vựng
    deleteTarget?.let { wordId ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Xoá từ vựng?") },
            text = { Text("Hành động này không thể hoàn tác.") },
            confirmButton = {
                TextButton(onClick = {
                    wordViewModel.deleteWord(wordId)
                    deleteTarget = null
                }) { Text("Xoá", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Huỷ") }
            }
        )
    }

    val exportWords = remember(state.wordItems) {
        state.wordItems.map { it.word }
    }

    fun handleExportResult(uri: android.net.Uri?, format: VocabFileFormat) {
        if (uri == null) {
            pendingExport = false
            return
        }
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    VocabFileIO.writeToUri(context, uri, exportWords, format)
                }
                Toast.makeText(
                    context,
                    "Đã lưu ${exportWords.size} từ vào Files",
                    Toast.LENGTH_LONG
                ).show()
                showExportDialog = false
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    e.localizedMessage ?: "Không thể lưu tệp",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                pendingExport = false
            }
        }
    }

    val saveCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(VocabFileIO.mimeType(VocabFileFormat.CSV))
    ) { uri -> handleExportResult(uri, VocabFileFormat.CSV) }

    val saveExcelLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(VocabFileIO.mimeType(VocabFileFormat.EXCEL))
    ) { uri -> handleExportResult(uri, VocabFileFormat.EXCEL) }

    LaunchedEffect(pendingExport) {
        if (!pendingExport) return@LaunchedEffect
        pendingExport = false
        val fileName = VocabFileIO.suggestedExportFileName(setName, exportFormat)
        when (exportFormat) {
            VocabFileFormat.CSV -> saveCsvLauncher.launch(fileName)
            VocabFileFormat.EXCEL -> saveExcelLauncher.launch(fileName)
        }
    }

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!pendingExport) showExportDialog = false
            },
            title = { Text("Xuất bộ từ vựng", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Chọn định dạng và lưu trực tiếp vào ứng dụng Files trên thiết bị.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${exportWords.size} từ sẽ được xuất",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = exportFormat == VocabFileFormat.CSV,
                            onClick = { exportFormat = VocabFileFormat.CSV },
                            label = { Text("CSV") },
                            enabled = !pendingExport
                        )
                        FilterChip(
                            selected = exportFormat == VocabFileFormat.EXCEL,
                            onClick = { exportFormat = VocabFileFormat.EXCEL },
                            label = { Text("Excel (.xlsx)") },
                            enabled = !pendingExport
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { pendingExport = true },
                    enabled = exportWords.isNotEmpty() && !pendingExport
                ) {
                    if (pendingExport) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(
                            Icons.Default.FileDownload,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Lưu vào Files")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showExportDialog = false },
                    enabled = !pendingExport
                ) {
                    Text("Huỷ")
                }
            }
        )
    }
}

@Composable
private fun WordCardItem(
    item: WordItemState,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val word = item.word
    val record = item.record

    // Xác định Badge chỉ số trạng thái học tập SM-2
    val (badgeText, badgeColor) = when (record?.status) {
        "MASTERED" -> "MASTERED" to Color(0xFF4CAF50)  // Xanh lá
        "REVIEW" -> "REVIEW" to Color(0xFFFF9800)      // Vàng cam
        "LEARNING" -> "LEARNING" to Color(0xFF2196F3)  // Xanh dương
        else -> "NEW" to Color(0xFF9E9E9E)             // Xám mặc định
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = word.word,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Hiển thị Badge trạng thái lặp lại ngắt quãng
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(badgeColor.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badgeText,
                            fontSize = 8.sp,
                            color = badgeColor,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                if (!word.pronunciation.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = word.pronunciation ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = word.meaning,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )

                if (!word.exampleSentence.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "\"${word.exampleSentence}\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }
            }

            Row {
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Sửa",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = onDelete) {
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
