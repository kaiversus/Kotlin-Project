package com.minlish.app.ui.vocab

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minlish.app.viewmodel.VocabViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VocabSetListScreen(
    vocabViewModel: VocabViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToWords: (setId: String, setName: String) -> Unit,
    onNavigateToCreate: () -> Unit
) {
    val state by vocabViewModel.uiState.collectAsState()
    var deleteTarget by remember { mutableStateOf<String?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showCreateOptions by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        vocabViewModel.loadSets()
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

                // Dropdown menu lựa chọn phương thức tạo
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
                        text = { Text("Nhập hàng loạt từ CSV") },
                        leadingIcon = { Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp)) },
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
            // 1. Thanh tìm kiếm (Search Bar)
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

            // 2. Bộ lọc phân loại nhanh (Filter Chips Row)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val tags = listOf("Tất cả", "IELTS", "Business", "Travel")
                tags.forEach { tag ->
                    FilterChip(
                        selected = state.selectedTag == tag,
                        onClick = { vocabViewModel.updateSelectedTag(tag) },
                        label = { Text(tag) },
                        shape = RoundedCornerShape(20.dp)
                    )
                }

                // Nút lọc Yêu thích
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

            // 3. Danh sách các bộ từ vựng
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
                                    modifier = Modifier.fillMaxWidth(),
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
                                                    
                                                    // Hiển thị nhãn Tag của bộ từ nếu có
                                                    if (set.tags != "[]") {
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        val cleanTags = set.tags.replace("[", "").replace("]", "").replace("\"", "")
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

                                            // Nút Trái tim Yêu thích
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

                                        // Hiển thị thanh tiến độ học tập (Progress Bar)
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

    // Hộp thoại Xóa bộ từ
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

    // Hộp thoại Nhập CSV (Import CSV Dialog)
    if (showImportDialog) {
        var importName by remember { mutableStateOf("") }
        var importDesc by remember { mutableStateOf("") }
        var importCsvText by remember { mutableStateOf("") }
        var hasError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Nhập bộ từ vựng từ CSV", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Định dạng dòng CSV chuẩn:\nTừ,Phiên âm,Nghĩa,Giải nghĩa,Ví dụ\nVí dụ:\nmeticulous,/məˈtɪk/,Tỉ mỉ,Precise,câu ví dụ 1",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = importName,
                        onValueChange = { importName = it },
                        label = { Text("Tên bộ từ") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = importDesc,
                        onValueChange = { importDesc = it },
                        label = { Text("Mô tả bộ từ (tùy chọn)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = importCsvText,
                        onValueChange = { importCsvText = it },
                        label = { Text("Nội dung văn bản CSV") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        placeholder = { Text("Nhập hoặc dán văn bản CSV vào đây...") }
                    )

                    if (hasError) {
                        Text(
                            text = "Vui lòng nhập đầy đủ tên bộ từ và dữ liệu CSV",
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
                        if (importName.isBlank() || importCsvText.isBlank()) {
                            hasError = true
                        } else {
                            hasError = false
                            vocabViewModel.importCsvToSet(
                                setName = importName,
                                setDescription = importDesc,
                                csvText = importCsvText,
                                onDone = { showImportDialog = false }
                            )
                        }
                    }
                ) {
                    Text("Nhập dữ liệu")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Huỷ")
                }
            }
        )
    }
}
