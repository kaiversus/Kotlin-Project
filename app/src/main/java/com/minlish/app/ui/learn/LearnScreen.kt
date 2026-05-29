package com.minlish.app.ui.learn

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Style
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.minlish.app.data.model.Word
import com.minlish.app.viewmodel.DailyPlanListType
import com.minlish.app.viewmodel.LearnViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearnScreen(
    displayName: String,
    learnViewModel: LearnViewModel,
    onOpenFlashcard: () -> Unit,
    onOpenQuiz: () -> Unit,
    onOpenTyping: () -> Unit,
    onOpenListening: () -> Unit,
    onOpenVocab: () -> Unit
) {
    val state by learnViewModel.uiState.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                learnViewModel.loadDailyPlan()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (state.activeList != null) {
        ModalBottomSheet(
            onDismissRequest = { learnViewModel.dismissDailyPlanList() },
            sheetState = sheetState
        ) {
            DailyPlanWordListSheet(
                type = state.activeList!!,
                words = state.listWords,
                isLoading = state.listLoading
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        LearnTopBar(displayName = displayName)
        DailyPlanCard(
            progress = state.progress,
            completedToday = state.completedToday,
            newWords = state.plannedNewCount,
            reviewWords = state.plannedReviewCount,
            dailyTarget = state.dailyTarget,
            onNewWordsClick = { learnViewModel.showDailyPlanList(DailyPlanListType.NEW) },
            onReviewClick = { learnViewModel.showDailyPlanList(DailyPlanListType.REVIEW) }
        )
        if (state.error != null) {
            Text(
                text = state.error ?: "",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
        LearningModes(
            onOpenFlashcard = onOpenFlashcard,
            onOpenQuiz = onOpenQuiz,
            onOpenTyping = onOpenTyping,
            onOpenListening = onOpenListening,
            onOpenVocab = onOpenVocab
        )
        TodayActivitySection(
            completedNewWords = state.completedNewWords,
            completedReviewWords = state.completedReviewWords
        )
        Spacer(modifier = Modifier.height(72.dp))
    }
}

@Composable
private fun LearnTopBar(displayName: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = displayName.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "MinLish",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = {}) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Notifications",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun DailyPlanCard(
    progress: Float,
    completedToday: Int,
    newWords: Int,
    reviewWords: Int,
    dailyTarget: Int,
    onNewWordsClick: () -> Unit,
    onReviewClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F4FF))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Your Daily Plan",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Bạn hoàn thành $completedToday/$dailyTarget mục tiêu hôm nay (${(progress * 100).toInt()}%)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Còn ${newWords + reviewWords} từ trong kế hoạch hôm nay ($newWords mới · $reviewWords ôn)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PlanStatChip(
                    label = "New words",
                    value = newWords.toString(),
                    onClick = onNewWordsClick
                )
                PlanStatChip(
                    label = "Review",
                    value = reviewWords.toString(),
                    onClick = onReviewClick
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(99.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color(0xFFE3E8FF)
            )
        }
    }
}

@Composable
private fun PlanStatChip(label: String, value: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE7ECFF))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DailyPlanWordListSheet(
    type: DailyPlanListType,
    words: List<Word>,
    isLoading: Boolean
) {
    val title = when (type) {
        DailyPlanListType.NEW -> "Từ mới trong kế hoạch hôm nay"
        DailyPlanListType.REVIEW -> "Từ ôn trong kế hoạch hôm nay"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 32.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = "${words.size} từ",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            words.isEmpty() -> {
                Text(
                    text = "Chưa có từ nào trong mục này hôm nay.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            }
            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    items(words, key = { it.id }) { word ->
                        DailyPlanWordItem(word = word)
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyPlanWordItem(word: Word) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F4FF))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = word.word,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            if (!word.pronunciation.isNullOrBlank()) {
                Text(
                    text = word.pronunciation,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = word.meaning,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun TodayActivitySection(
    completedNewWords: List<Word>,
    completedReviewWords: List<Word>
) {
    val allCompleted = buildList {
        completedNewWords.forEach { add(it to "New") }
        completedReviewWords.forEach { add(it to "Review") }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Hoạt động hôm nay",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "${allCompleted.size} từ đã học/ôn",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (allCompleted.isEmpty()) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F4FF))
            ) {
                Text(
                    text = "Chưa có từ nào được học hoặc ôn hôm nay.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                allCompleted.forEach { (word, tag) ->
                    TodayActivityWordItem(word = word, tag = tag)
                }
            }
        }
    }
}

@Composable
private fun TodayActivityWordItem(word: Word, tag: String) {
    val tagColor = if (tag == "New") Color(0xFF4F46E5) else Color(0xFF006A61)
    val tagBg = if (tag == "New") Color(0xFFE2DFFF) else Color(0xFF86F2E4)

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F4FF))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = word.word,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = word.meaning,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = tagBg
            ) {
                Text(
                    text = tag,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = tagColor
                )
            }
        }
    }
}

@Composable
private fun LearningModes(
    onOpenFlashcard: () -> Unit,
    onOpenQuiz: () -> Unit,
    onOpenTyping: () -> Unit,
    onOpenListening: () -> Unit,
    onOpenVocab: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Learning Modes",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            ModeCard(
                title = "Flashcard",
                icon = Icons.Default.Style,
                highlight = true,
                modifier = Modifier.weight(1f),
                onClick = onOpenFlashcard
            )
            ModeCard(
                title = "Multiple Choice",
                icon = Icons.Default.Quiz,
                highlight = false,
                modifier = Modifier.weight(1f),
                onClick = onOpenQuiz
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            ModeCard(
                title = "Typing Practice",
                icon = Icons.Default.Keyboard,
                highlight = false,
                modifier = Modifier.weight(1f),
                onClick = onOpenTyping
            )
            ModeCard(
                title = "Listening",
                icon = Icons.Default.Headphones,
                highlight = false,
                modifier = Modifier.weight(1f),
                onClick = onOpenListening
            )
        }
    }
}

@Composable
private fun ModeCard(
    title: String,
    icon: ImageVector,
    highlight: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (highlight) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                else Color(0xFFE6E6EE)
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (highlight) MaterialTheme.colorScheme.primaryContainer
                        else Color(0xFFF2F4FF)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = if (highlight) MaterialTheme.colorScheme.primary else Color(0xFF596078)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

