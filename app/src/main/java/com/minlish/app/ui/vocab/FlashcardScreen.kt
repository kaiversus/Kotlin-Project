package com.minlish.app.ui.vocab

import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.minlish.app.viewmodel.LearningViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardScreen(
    setId: String,
    learningViewModel: LearningViewModel,
    onNavigateBack: () -> Unit
) {
    val state by learningViewModel.state.collectAsState()

    LaunchedEffect(setId) { learningViewModel.startSession(setId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (!state.isFinished && state.total > 0) {
                        Text("${state.progress + 1} / ${state.total}")
                    } else {
                        Text("Flashcard")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        learningViewModel.reset()
                        onNavigateBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.Center
        ) {
            when {
                state.isLoading -> CircularProgressIndicator()

                state.isFinished -> FinishedScreen(
                    total = state.total,
                    correct = state.correctCount,
                    onRestart = { learningViewModel.startSession(setId) },
                    onBack = {
                        learningViewModel.reset()
                        onNavigateBack()
                    }
                )

                state.currentWord != null -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Progress bar
                        if (state.total > 0) {
                            LinearProgressIndicator(
                                progress = { state.progress.toFloat() / state.total },
                                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                            )
                        }

                        // Flip card
                        FlipCard(
                            isFlipped = state.isFlipped,
                            word = state.currentWord!!,
                            onClick = { learningViewModel.flipCard() }
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        if (!state.isFlipped) {
                            Text(
                                "Nhấn vào thẻ để xem nghĩa",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                "Bạn nhớ từ này như thế nào?",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                GradeButton(modifier = Modifier.weight(1f),
                                    label = "Lại", color = MaterialTheme.colorScheme.error,
                                    onClick = { learningViewModel.grade(0) })
                                GradeButton(modifier = Modifier.weight(1f),
                                    label = "Khó", color = MaterialTheme.colorScheme.tertiary,
                                    onClick = { learningViewModel.grade(1) })
                                GradeButton(modifier = Modifier.weight(1f),
                                    label = "Ổn", color = MaterialTheme.colorScheme.primary,
                                    onClick = { learningViewModel.grade(2) })
                                GradeButton(modifier = Modifier.weight(1f),
                                    label = "Dễ", color = MaterialTheme.colorScheme.secondary,
                                    onClick = { learningViewModel.grade(3) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FlipCard(
    isFlipped: Boolean,
    word: com.minlish.app.data.model.Word,
    onClick: () -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "flip"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .graphicsLayer { rotationY = rotation; cameraDistance = 12f * density }
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { if (rotation > 90f) rotationY = 180f },
            contentAlignment = Alignment.Center
        ) {
            if (rotation <= 90f) {
                // Front: word
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = word.word,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    if (!word.pronunciation.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = word.pronunciation,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // Back: meaning + details
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp)
                ) {
                    Text(
                        text = word.meaning,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (!word.exampleSentence.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "\"${word.exampleSentence}\"",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (!word.collocation.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = word.collocation,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GradeButton(
    modifier: Modifier = Modifier,
    label: String,
    color: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun FinishedScreen(
    total: Int,
    correct: Int,
    onRestart: () -> Unit,
    onBack: () -> Unit
) {
    val accuracy = if (total > 0) (correct * 100 / total) else 0
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(24.dp)
    ) {
        Text("Hoàn thành! 🎉",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold)
        Text("$correct / $total từ đúng ($accuracy%)",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onRestart, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text("Học lại")
        }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text("Quay về")
        }
    }
}
