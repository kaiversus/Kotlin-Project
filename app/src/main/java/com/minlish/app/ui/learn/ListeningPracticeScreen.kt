package com.minlish.app.ui.learn

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minlish.app.viewmodel.ListeningOption
import com.minlish.app.viewmodel.ListeningViewModel
import com.minlish.app.viewmodel.QuizCheckResult

private val ListenPrimary = Color(0xFF3525CD)
private val ListenPrimaryContainer = Color(0xFF4F46E5)
private val ListenPrimaryFixed = Color(0xFFE2DFFF)
private val ListenSecondary = Color(0xFF006A61)
private val ListenSecondaryContainer = Color(0xFF86F2E4)
private val ListenTertiary = Color(0xFF684000)
private val ListenSurface = Color(0xFFF8F9FF)
private val ListenSurfaceContainer = Color(0xFFE5EEFF)
private val ListenSurfaceContainerHigh = Color(0xFFDCE9FF)
private val ListenSurfaceVariant = Color(0xFFD3E4FE)
private val ListenOnSurfaceVariant = Color(0xFF464555)
private val ListenOutline = Color(0xFF777587)
private val ListenError = Color(0xFFBA1A1A)

private val WaveDelays = listOf(100, 300, 200, 500, 400, 600, 200, 400)

@Composable
fun ListeningPracticeScreen(
    setId: String,
    displayName: String,
    listeningViewModel: ListeningViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToVocab: () -> Unit
) {
    val state by listeningViewModel.state.collectAsState()
    val context = LocalContext.current
    val speaker = remember { WordSpeaker(context) }

    DisposableEffect(Unit) {
        onDispose { speaker.shutdown() }
    }

    LaunchedEffect(setId) { listeningViewModel.startSession(setId) }

    LaunchedEffect(state.isPlaying, state.currentWord?.id) {
        val word = state.currentWord
        if (state.isPlaying && word != null) {
            speaker.speakWord(word) { listeningViewModel.onAudioFinished() }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ListenSurface)
    ) {
        ListeningHeader(
            progress = state.progress,
            total = state.total,
            correctCount = state.correctCount,
            displayName = displayName,
            onClose = {
                speaker.stop()
                listeningViewModel.reset()
                onNavigateBack()
            }
        )

        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = ListenPrimary)
                }
            }
            state.isFinished -> {
                ListeningFinishedScreen(
                    total = state.total,
                    correct = state.correctCount,
                    isTodayReviewSession = state.isTodayReviewSession,
                    onNavigateToVocab = onNavigateToVocab,
                    onBack = {
                        listeningViewModel.reset()
                        onNavigateBack()
                    }
                )
            }
            state.currentWord != null -> {
                ListeningContent(
                    isPlaying = state.isPlaying,
                    options = state.options,
                    selectedOptionIndex = state.selectedOptionIndex,
                    correctOptionIndex = state.correctOptionIndex,
                    checkResult = state.checkResult,
                    hintText = state.hintText,
                    onPlay = listeningViewModel::playAudio,
                    onSelectOption = listeningViewModel::selectOption,
                    onHint = listeningViewModel::showHint,
                    onCheck = listeningViewModel::checkAnswer,
                    onSkip = listeningViewModel::skipWord
                )
            }
        }
    }
}

@Composable
private fun ListeningHeader(
    progress: Int,
    total: Int,
    correctCount: Int,
    displayName: String,
    onClose: () -> Unit
) {
    Surface(color = ListenSurface, shadowElevation = 2.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = ListenOnSurfaceVariant)
            }
            Text(
                text = "$progress/$total",
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = ListenOnSurfaceVariant
            )
            Icon(Icons.Default.Favorite, contentDescription = null, tint = ListenTertiary, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.size(4.dp))
            Text(
                text = correctCount.toString(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = ListenTertiary
            )
            Spacer(modifier = Modifier.size(8.dp))
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(ListenPrimaryFixed),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = displayName.take(1).uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = ListenPrimary
                )
            }
        }
    }
}

@Composable
private fun ListeningContent(
    isPlaying: Boolean,
    options: List<ListeningOption>,
    selectedOptionIndex: Int?,
    correctOptionIndex: Int,
    checkResult: QuizCheckResult,
    hintText: String?,
    onPlay: () -> Unit,
    onSelectOption: (Int) -> Unit,
    onHint: () -> Unit,
    onCheck: () -> Unit,
    onSkip: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 24.dp, bottom = 120.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Listen and identify",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0B1C30),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Tap the button to hear the audio clip",
                style = MaterialTheme.typography.bodyMedium,
                color = ListenOnSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            AudioPlayCard(isPlaying = isPlaying, onPlay = onPlay)

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                options.forEachIndexed { index, option ->
                    ListeningOptionCard(
                        option = option,
                        isSelected = selectedOptionIndex == index,
                        isCorrect = checkResult != QuizCheckResult.NONE && index == correctOptionIndex,
                        isWrong = checkResult == QuizCheckResult.WRONG && selectedOptionIndex == index,
                        enabled = checkResult != QuizCheckResult.CORRECT,
                        onClick = { onSelectOption(index) }
                    )
                }
            }

            if (hintText != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Hint: $hintText",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ListenPrimaryContainer,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = onHint) {
                Icon(Icons.Default.Lightbulb, contentDescription = null, tint = ListenPrimary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.size(6.dp))
                Text("I need a hint", color = ListenPrimary, fontWeight = FontWeight.SemiBold)
            }
        }

        ListeningFooter(
            modifier = Modifier.align(Alignment.BottomCenter),
            hasSelection = selectedOptionIndex != null,
            checkResult = checkResult,
            onCheck = onCheck,
            onSkip = onSkip
        )
    }
}

@Composable
private fun AudioPlayCard(
    isPlaying: Boolean,
    onPlay: () -> Unit
) {
    val buttonScale by animateFloatAsState(
        targetValue = if (isPlaying) 1.05f else 1f,
        animationSpec = tween(300),
        label = "playScale"
    )
    val rippleTransition = rememberInfiniteTransition(label = "ripple")
    val rippleScale by rippleTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isPlaying) 1.8f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000),
            repeatMode = RepeatMode.Restart
        ),
        label = "rippleScale"
    )
    val rippleAlpha by rippleTransition.animateFloat(
        initialValue = if (isPlaying) 0.4f else 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000),
            repeatMode = RepeatMode.Restart
        ),
        label = "rippleAlpha"
    )

    Surface(
        shape = RoundedCornerShape(32.dp),
        color = Color.White,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(32.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 48.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(400.dp)
                    .alpha(0.03f)
                    .border(20.dp, ListenPrimary, CircleShape)
            )
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .alpha(0.03f)
                    .border(10.dp, ListenPrimary, CircleShape)
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(contentAlignment = Alignment.Center) {
                    if (isPlaying) {
                        Box(
                            modifier = Modifier
                                .size(128.dp)
                                .scale(rippleScale)
                                .alpha(rippleAlpha)
                                .background(ListenPrimaryContainer, CircleShape)
                        )
                    }
                    IconButton(
                        onClick = onPlay,
                        enabled = !isPlaying,
                        modifier = Modifier
                            .size(128.dp)
                            .scale(buttonScale)
                            .background(ListenPrimary, CircleShape)
                            .shadow(8.dp, CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.VolumeUp else Icons.Default.PlayArrow,
                            contentDescription = "Play audio",
                            tint = Color.White,
                            modifier = Modifier.size(64.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                WaveVisualizer(
                    isActive = isPlaying,
                    modifier = Modifier.alpha(if (isPlaying) 1f else 0.3f)
                )
            }
        }
    }
}

@Composable
private fun WaveVisualizer(
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.height(48.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        WaveDelays.forEachIndexed { index, delayMs ->
            val transition = rememberInfiniteTransition(label = "wave$index")
            val height by transition.animateFloat(
                initialValue = 16f,
                targetValue = 48f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1200, delayMillis = delayMs),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "barHeight$index"
            )
            Box(
                modifier = Modifier
                    .size(width = 6.dp, height = if (isActive) height.dp else 16.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(ListenPrimary)
            )
        }
    }
}

@Composable
private fun ListeningOptionCard(
    option: ListeningOption,
    isSelected: Boolean,
    isCorrect: Boolean,
    isWrong: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val borderColor = when {
        isCorrect -> ListenSecondary
        isWrong -> ListenError
        isSelected -> ListenPrimary
        else -> Color.Transparent
    }
    val backgroundColor = when {
        isCorrect -> ListenPrimaryFixed.copy(alpha = 0.35f)
        isWrong -> Color(0xFFFFDAD6)
        isSelected -> ListenPrimaryFixed
        else -> Color.White
    }
    val labelBorder = when {
        isSelected || isCorrect -> ListenPrimary
        else -> ListenOutline
    }
    val labelBg = when {
        isSelected || isCorrect -> ListenPrimary
        else -> Color.Transparent
    }
    val labelFg = when {
        isSelected || isCorrect -> Color.White
        else -> ListenOnSurfaceVariant
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp))
            .border(2.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(labelBg)
                    .border(2.dp, labelBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = option.label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = labelFg
                )
            }
            Text(
                text = option.word.word,
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF0B1C30),
                modifier = Modifier.weight(1f)
            )
            if (isSelected || isCorrect) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = ListenPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun ListeningFooter(
    modifier: Modifier = Modifier,
    hasSelection: Boolean,
    checkResult: QuizCheckResult,
    onCheck: () -> Unit,
    onSkip: () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onSkip,
                enabled = checkResult != QuizCheckResult.CORRECT,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ListenOutline),
                modifier = Modifier.height(52.dp)
            ) {
                Text("Skip", fontWeight = FontWeight.SemiBold, color = ListenOnSurfaceVariant)
            }

            val buttonConfig = when (checkResult) {
                QuizCheckResult.CORRECT -> FooterButtonConfig(
                    text = "Correct!",
                    colors = ButtonDefaults.buttonColors(containerColor = ListenSecondary, contentColor = Color.White),
                    enabled = true,
                    showCheckIcon = true
                )
                QuizCheckResult.WRONG -> FooterButtonConfig(
                    text = "Try Again",
                    colors = ButtonDefaults.buttonColors(containerColor = ListenError, contentColor = Color.White),
                    enabled = true,
                    showCheckIcon = false
                )
                QuizCheckResult.NONE -> if (hasSelection) {
                    FooterButtonConfig(
                        text = "Check Answer",
                        colors = ButtonDefaults.buttonColors(containerColor = ListenPrimary, contentColor = Color.White),
                        enabled = true,
                        showCheckIcon = false
                    )
                } else {
                    FooterButtonConfig(
                        text = "Check Answer",
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ListenOutline,
                            contentColor = Color.White,
                            disabledContainerColor = ListenOutline,
                            disabledContentColor = Color.White.copy(alpha = 0.7f)
                        ),
                        enabled = false,
                        showCheckIcon = false
                    )
                }
            }

            Button(
                onClick = onCheck,
                enabled = buttonConfig.enabled,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = buttonConfig.colors
            ) {
                if (buttonConfig.showCheckIcon) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.size(8.dp))
                }
                Text(buttonConfig.text, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

private data class FooterButtonConfig(
    val text: String,
    val colors: androidx.compose.material3.ButtonColors,
    val enabled: Boolean,
    val showCheckIcon: Boolean
)

@Composable
private fun ListeningFinishedScreen(
    total: Int,
    correct: Int,
    isTodayReviewSession: Boolean,
    onNavigateToVocab: () -> Unit,
    onBack: () -> Unit
) {
    val accuracy = if (total > 0) (correct * 100 / total) else 0
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (isTodayReviewSession) "Ôn xong!" else "Hoàn thành!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = ListenPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (total > 0) {
            Text(
                text = "$correct / $total câu đúng ($accuracy%)",
                style = MaterialTheme.typography.titleMedium,
                color = ListenSecondary
            )
        } else {
            Text(
                text = "Chưa có từ nào học hôm nay",
                style = MaterialTheme.typography.titleMedium,
                color = ListenOnSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onNavigateToVocab,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ListenPrimaryContainer)
        ) {
            Text("Học từ mới")
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text("Quay về", color = ListenPrimary)
        }
    }
}
