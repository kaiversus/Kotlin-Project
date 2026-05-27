package com.minlish.app.ui.vocab

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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

    LaunchedEffect(Unit) { vocabViewModel.loadSets() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bộ từ vựng của tôi") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToCreate) {
                Icon(Icons.Default.Add, contentDescription = "Tạo bộ từ")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                state.sets.isEmpty() -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Chưa có bộ từ vựng nào",
                            style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Nhấn + để tạo bộ từ đầu tiên",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.sets) { set ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(set.name, fontWeight = FontWeight.SemiBold,
                                            style = MaterialTheme.typography.titleMedium)
                                        if (!set.description.isNullOrBlank()) {
                                            Text(set.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("${set.totalWords} từ",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary)
                                    }
                                    IconButton(onClick = {
                                        onNavigateToWords(set.id, set.name)
                                    }) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = "Mở",
                                            tint = MaterialTheme.colorScheme.primary)
                                    }
                                    IconButton(onClick = { deleteTarget = set.id }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Xoá",
                                            tint = MaterialTheme.colorScheme.error)
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
            text = { Text("Hành động này không thể hoàn tác.") },
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
}
