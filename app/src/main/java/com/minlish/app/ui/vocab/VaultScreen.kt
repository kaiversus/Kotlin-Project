package com.minlish.app.ui.vocab

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minlish.app.data.repository.LearningRepository
import com.minlish.app.ui.learn.WordSpeaker
import com.minlish.app.viewmodel.VaultFilter
import com.minlish.app.viewmodel.VaultViewModel
import com.minlish.app.viewmodel.WordItemState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(
    userId: String,
    vaultViewModel: VaultViewModel,
    onNavigateBack: () -> Unit
) {
    val state by vaultViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val speaker = remember { WordSpeaker(context) }
    val timeRepo = remember { LearningRepository() }

    LaunchedEffect(userId) {
        vaultViewModel.loadLearnedWords(userId)
    }

    DisposableEffect(Unit) {
        onDispose {
            speaker.shutdown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kho từ vựng đã học", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Search Bar
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = vaultViewModel::onSearchQueryChange,
                placeholder = { Text("Tìm kiếm từ vựng...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { vaultViewModel.onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            // Filtering Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VaultFilter.entries.forEach { filter ->
                    val isSelected = state.selectedFilter == filter
                    val label = when (filter) {
                        VaultFilter.ALL -> "All"
                        VaultFilter.LEARNING -> "Learning"
                        VaultFilter.REVIEW -> "Review"
                        VaultFilter.MASTERED -> "Mastered"
                    }
                    FilterChip(
                        selected = isSelected,
                        onClick = { vaultViewModel.onFilterChange(filter) },
                        label = { Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium) },
                        shape = RoundedCornerShape(999.dp)
                    )
                }
            }

            // Learned Words List
            if (state.isLoading) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (state.words.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (state.searchQuery.isNotEmpty()) "Không tìm thấy từ nào phù hợp." else "Bạn chưa học từ vựng nào.",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.words, key = { it.word.id }) { item ->
                        VaultWordCard(
                            item = item,
                            timeFormatter = timeRepo::formatTimeRemaining,
                            onSpeakClick = { speaker.speakWord(item.word) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VaultWordCard(
    item: WordItemState,
    timeFormatter: (Long) -> String,
    onSpeakClick: () -> Unit
) {
    val record = item.record ?: return
    val word = item.word

    // Badge styling based on status
    val (statusLabel, badgeBgColor, badgeTextColor) = when (record.status) {
        "LEARNING" -> Triple("LEARNING", Color(0xFFFFE0B2), Color(0xFFE65100))
        "REVIEW" -> Triple("REVIEW", Color(0xFFBBDEFB), Color(0xFF0D47A1))
        "MASTERED" -> Triple("MASTERED", Color(0xFFC8E6C9), Color(0xFF1B5E20))
        else -> Triple("NEW", Color(0xFFF5F5F5), Color(0xFF616161))
    }

    val timeRemaining = timeFormatter(record.nextReviewDate)
    val isDue = timeRemaining == "Cần ôn ngay"
    val timeBadgeBg = if (isDue) Color(0xFFFFDAD6) else MaterialTheme.colorScheme.surfaceVariant
    val timeBadgeText = if (isDue) Color(0xFFBA1A1A) else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // Word and Status Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = word.word,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = badgeBgColor,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Text(
                            text = statusLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeTextColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                // Pronunciation
                word.pronunciation?.takeIf { it.isNotBlank() }?.let { ipa ->
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = ipa,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }

                // Meaning
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = word.meaning,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Next Review Status
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Ôn tập:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = timeBadgeBg
                    ) {
                        Text(
                            text = timeRemaining,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = timeBadgeText,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // Speak Button
            IconButton(
                onClick = onSpeakClick,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "Phát âm",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}
