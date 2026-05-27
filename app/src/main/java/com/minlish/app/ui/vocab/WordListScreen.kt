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
import com.minlish.app.viewmodel.WordViewModel

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
    var deleteTarget by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(setId) { wordViewModel.loadWords(setId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(setName) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                actions = {
                    if (state.words.isNotEmpty()) {
                        IconButton(onClick = { onNavigateToFlashcard(setId) }) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Học ngay",
                                tint = MaterialTheme.colorScheme.primary)
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
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                state.words.isEmpty() -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Chưa có từ vựng nào",
                            style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Nhấn + để thêm từ đầu tiên",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                else -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Start learning button
                        Button(
                            onClick = { onNavigateToFlashcard(setId) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Học Flashcard (${state.words.size} từ)")
                        }

                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(state.words) { word ->
                                Card(
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(word.word,
                                                fontWeight = FontWeight.SemiBold,
                                                style = MaterialTheme.typography.titleSmall)
                                            if (!word.pronunciation.isNullOrBlank()) {
                                                Text(word.pronunciation,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            Text(word.meaning,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.primary)
                                            if (!word.exampleSentence.isNullOrBlank()) {
                                                Text("\"${word.exampleSentence}\"",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 2)
                                            }
                                        }
                                        IconButton(onClick = { deleteTarget = word.id }) {
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
    }

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
}
