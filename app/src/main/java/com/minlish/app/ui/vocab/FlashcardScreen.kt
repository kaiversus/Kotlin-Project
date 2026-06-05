package com.minlish.app.ui.vocab

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minlish.app.data.model.Word
import com.minlish.app.ui.learn.WordSpeaker
import com.minlish.app.viewmodel.LearningViewModel

private val FlashPrimary = Color(0xFF3525CD)
private val FlashPrimaryContainer = Color(0xFF4F46E5)
private val FlashPrimaryFixed = Color(0xFFE2DFFF)
private val FlashSecondary = Color(0xFF006A61)
private val FlashSecondaryFixed = Color(0xFF89F5E7)
private val FlashTertiaryFixed = Color(0xFFFFDBB8)
private val FlashErrorContainer = Color(0xFFFFDAD6)
private val FlashSurface = Color(0xFFF8F9FF)
private val FlashSurfaceContainerHighest = Color(0xFFD3E4FF)
private val FlashSurfaceContainerLow = Color(0xFFEFF4FF)
private val FlashOnSurfaceVariant = Color(0xFF464555)

@Composable
fun FlashcardScreen(
    setId: String,
    displayName: String,
    learningViewModel: LearningViewModel,
    onNavigateBack: () -> Unit
) {
    val state by learningViewModel.state.collectAsState()
    val context = LocalContext.current
    val speaker = remember { WordSpeaker(context) }

    DisposableEffect(Unit) {
        onDispose { speaker.shutdown() }
    }

    LaunchedEffect(setId) { learningViewModel.startSession(setId) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FlashSurface)
    ) {
        FlashcardHeader(
            progress = state.progress,
            total = state.total,
            correctCount = state.correctCount,
            displayName = displayName,
            onClose = {
                speaker.stop()
                learningViewModel.reset()
                onNavigateBack()
            }
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            when {
                state.isLoading -> CircularProgressIndicator(color = FlashPrimary)
                state.isFinished -> FinishedScreen(
                    total = state.total,
                    correct = state.correctCount,
                    isTodayReviewSession = state.isTodayReviewSession,
                    canReviewToday = state.canReviewToday,
                    todayWordsCount = state.todayWordsCount,
                    onReviewToday = { learningViewModel.startTodayReviewSession(setId) },
                    onBack = {
                        learningViewModel.reset()
                        onNavigateBack()
                    }
                )
                state.currentWord != null -> FlipCard(
                    isFlipped = state.isFlipped,
                    word = state.currentWord!!,
                    onClick = { learningViewModel.flipCard() },
                    onSpeakWord = { speaker.speakWord(it) }
                )
            }
        }

        FlashcardFooter(
            isFlipped = state.isFlipped,
            isVisible = !state.isLoading && !state.isFinished && state.currentWord != null,
            onGrade = { learningViewModel.grade(it) },
            intervalLabel = { learningViewModel.previewIntervalLabel(it) }
        )
    }
}

@Composable
private fun FlashcardHeader(
    progress: Int,
    total: Int,
    correctCount: Int,
    displayName: String,
    onClose: () -> Unit
) {
    Surface(
        color = FlashSurface,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close",
                    tint = FlashOnSurfaceVariant
                )
            }

            Text(
                text = "$progress/$total",
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = FlashOnSurfaceVariant
            )

            Icon(
                Icons.Default.Favorite,
                contentDescription = null,
                tint = FlashSecondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.size(4.dp))
            Text(
                text = correctCount.toString(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = FlashOnSurfaceVariant
            )

            Spacer(modifier = Modifier.size(8.dp))

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(FlashPrimaryFixed),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = displayName.take(1).uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = FlashPrimary
                )
            }
        }
    }
}

@Composable
private fun FlipCard(
    isFlipped: Boolean,
    word: Word,
    onClick: () -> Unit,
    onSpeakWord: (Word) -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 600),
        label = "flip"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(420.dp)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            }
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isFlipped) 2.dp else 1.dp,
            color = if (isFlipped) FlashPrimaryContainer else Color(0x33C7C4D8)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    if (rotation > 90f) rotationY = 180f
                }
        ) {
            if (rotation <= 90f) {
                CardFront(word = word, onSpeakWord = onSpeakWord)
            } else {
                CardBack(word = word, onSpeakWord = onSpeakWord)
            }
        }
    }
}

@Composable
private fun CardFront(
    word: Word,
    onSpeakWord: (Word) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = word.word,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = FlashPrimary,
                textAlign = TextAlign.Center,
                lineHeight = 48.sp
            )
            IconButton(
                onClick = { onSpeakWord(word) },
                modifier = Modifier
                    .size(56.dp)
                    .background(FlashPrimaryFixed, CircleShape)
            ) {
                Icon(
                    Icons.Default.VolumeUp,
                    contentDescription = "Play audio",
                    tint = FlashPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Text(
            text = "TAP TO REVEAL",
            modifier = Modifier.align(Alignment.BottomCenter),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = FlashOnSurfaceVariant.copy(alpha = 0.6f),
            letterSpacing = 2.sp
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CardBack(
    word: Word,
    onSpeakWord: (Word) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column {
                Text(
                    text = word.word,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = FlashPrimary
                )
                if (!word.pronunciation.isNullOrBlank()) {
                    Text(
                        text = word.pronunciation,
                        style = MaterialTheme.typography.labelMedium,
                        color = FlashOnSurfaceVariant,
                        fontStyle = FontStyle.Italic
                    )
                }
            }
            IconButton(
                onClick = { onSpeakWord(word) },
                modifier = Modifier
                    .size(40.dp)
                    .background(FlashPrimaryFixed, CircleShape)
            ) {
                Icon(
                    Icons.Default.VolumeUp,
                    contentDescription = "Play audio",
                    tint = FlashPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "DEFINITION",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = FlashOnSurfaceVariant,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = word.description?.takeIf { it.isNotBlank() } ?: word.meaning,
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFF0B1C30)
        )

        if (word.description?.isNotBlank() == true) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = word.meaning,
                style = MaterialTheme.typography.bodyMedium,
                color = FlashPrimary,
                fontWeight = FontWeight.SemiBold
            )
        }

        if (!word.exampleSentence.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(16.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = FlashSurfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                ) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .fillMaxHeight()
                            .padding(vertical = 12.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(FlashPrimaryContainer.copy(alpha = 0.35f))
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Text(
                            text = "EXAMPLE SENTENCE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = FlashOnSurfaceVariant,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "\"${word.exampleSentence}\"",
                            style = MaterialTheme.typography.bodyMedium,
                            fontStyle = FontStyle.Italic,
                            color = Color(0xFF0B1C30)
                        )
                    }
                }
            }
        }

        if (!word.collocation.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "COMMON COLLOCATIONS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = FlashOnSurfaceVariant,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                word.collocation.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { item ->
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = FlashSurfaceContainerLow
                    ) {
                        Text(
                            text = item,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF3323CC)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FlashcardFooter(
    isFlipped: Boolean,
    isVisible: Boolean,
    onGrade: (Int) -> Unit,
    intervalLabel: (Int) -> String
) {
    Surface(
        color = FlashSurface,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isVisible && isFlipped) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GradeButton(
                        modifier = Modifier.weight(1f),
                        label = "Again",
                        interval = intervalLabel(0),
                        containerColor = FlashErrorContainer,
                        contentColor = Color(0xFF93000A),
                        onClick = { onGrade(0) }
                    )
                    GradeButton(
                        modifier = Modifier.weight(1f),
                        label = "Hard",
                        interval = intervalLabel(1),
                        containerColor = FlashTertiaryFixed,
                        contentColor = Color(0xFF2A1700),
                        onClick = { onGrade(1) }
                    )
                    GradeButton(
                        modifier = Modifier.weight(1f),
                        label = "Good",
                        interval = intervalLabel(2),
                        containerColor = FlashSecondaryFixed,
                        contentColor = Color(0xFF005049),
                        onClick = { onGrade(2) }
                    )
                    GradeButton(
                        modifier = Modifier.weight(1f),
                        label = "Easy",
                        interval = intervalLabel(3),
                        containerColor = FlashPrimaryFixed,
                        contentColor = Color(0xFF3323CC),
                        onClick = { onGrade(3) }
                    )
                }
            } else if (isVisible) {
                Text(
                    text = "Tap card to see definition",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = FlashOnSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}

@Composable
private fun GradeButton(
    modifier: Modifier = Modifier,
    label: String,
    interval: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(64.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            if (interval.isNotBlank()) {
                Text(
                    interval,
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun FinishedScreen(
    total: Int,
    correct: Int,
    isTodayReviewSession: Boolean,
    canReviewToday: Boolean,
    todayWordsCount: Int,
    onReviewToday: () -> Unit,
    onBack: () -> Unit
) {
    val accuracy = if (total > 0) (correct * 100 / total) else 0
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(24.dp)
    ) {
        Text(
            text = if (isTodayReviewSession) "Ôn xong!" else "Hoàn thành!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = FlashPrimary
        )
        if (total > 0) {
            Text(
                text = "$correct / $total từ đúng ($accuracy%)",
                style = MaterialTheme.typography.titleMedium,
                color = FlashSecondary
            )
        } else {
            Text(
                text = "Chưa có từ nào học hôm nay",
                style = MaterialTheme.typography.titleMedium,
                color = FlashOnSurfaceVariant
            )
        }
        if (canReviewToday) {
            Text(
                text = "Có $todayWordsCount từ hôm nay để ôn lại",
                style = MaterialTheme.typography.bodyMedium,
                color = FlashOnSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        if (canReviewToday) {
            Button(
                onClick = onReviewToday,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FlashPrimaryContainer)
            ) {
                Text("Ôn lại từ hôm nay")
            }
        }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text("Quay về", color = FlashPrimary)
        }
    }
}
