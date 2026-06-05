package com.minlish.app.ui.learn

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minlish.app.data.model.Word
import com.minlish.app.viewmodel.TypingFeedback
import com.minlish.app.viewmodel.TypingViewModel

private val TypingPrimary = Color(0xFF3525CD)
private val TypingPrimaryContainer = Color(0xFF4F46E5)
private val TypingPrimaryFixed = Color(0xFFE2DFFF)
private val TypingSecondary = Color(0xFF006A61)
private val TypingSurface = Color(0xFFF8F9FF)
private val TypingSurfaceContainer = Color(0xFFE5EEFF)
private val TypingSurfaceContainerHigh = Color(0xFFDCE9FF)
private val TypingSurfaceContainerLow = Color(0xFFEFF4FF)
private val TypingSurfaceContainerHighest = Color(0xFFD3E4FF)
private val TypingOnSurfaceVariant = Color(0xFF464555)
private val TypingOutlineVariant = Color(0xFFC7C4D8)
private val TypingOutline = Color(0xFF777587)

@Composable
fun TypingPracticeScreen(
    setId: String,
    typingViewModel: TypingViewModel,
    onNavigateBack: () -> Unit
) {
    val state by typingViewModel.state.collectAsState()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(setId) { typingViewModel.startSession(setId) }

    LaunchedEffect(state.currentWord?.id, state.feedback) {
        if (state.currentWord != null && state.feedback == null) {
            focusRequester.requestFocus()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TypingSurface)
            .imePadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TypingHeader(
                progress = state.progress,
                total = state.total,
                correctCount = state.correctCount,
                onClose = {
                    typingViewModel.reset()
                    onNavigateBack()
                }
            )

            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = TypingPrimary)
                    }
                }
                state.isFinished -> {
                    TypingFinishedScreen(
                        total = state.total,
                        correct = state.correctCount,
                        isTodayReviewSession = state.isTodayReviewSession,
                        canReviewToday = state.canReviewToday,
                        todayWordsCount = state.todayWordsCount,
                        onReviewToday = { typingViewModel.startTodayReviewSession(setId) },
                        onBack = {
                            typingViewModel.reset()
                            onNavigateBack()
                        }
                    )
                }
                state.currentWord != null -> {
                    TypingContent(
                        word = state.currentWord!!,
                        userInput = state.userInput,
                        hintText = state.hintText,
                        isWrong = state.isWrong,
                        focusRequester = focusRequester,
                        onInputChange = typingViewModel::onInputChange,
                        onSubmit = typingViewModel::submitAnswer,
                        onHint = typingViewModel::showHint,
                        onSkip = typingViewModel::skipWord
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = state.feedback == TypingFeedback.CORRECT,
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            CorrectFeedbackOverlay(onContinue = typingViewModel::continueAfterCorrect)
        }
    }
}

@Composable
private fun TypingHeader(
    progress: Int,
    total: Int,
    correctCount: Int,
    onClose: () -> Unit
) {
    Surface(color = TypingSurface, shadowElevation = 2.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = TypingOnSurfaceVariant)
            }
            Text(
                text = "$progress/$total",
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = TypingOnSurfaceVariant
            )
            Icon(
                Icons.Default.Favorite,
                contentDescription = null,
                tint = TypingSecondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.size(4.dp))
            Text(
                text = correctCount.toString(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TypingContent(
    word: Word,
    userInput: String,
    hintText: String?,
    isWrong: Boolean,
    focusRequester: FocusRequester,
    onInputChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onHint: () -> Unit,
    onSkip: () -> Unit
) {
    val definition = word.description?.takeIf { it.isNotBlank() } ?: word.meaning
    val isMatch = userInput.trim().equals(word.word, ignoreCase = true)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "DEFINITION",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = TypingOnSurfaceVariant,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = definition,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0B1C30),
                    lineHeight = 32.sp
                )
            }
            IconButton(
                onClick = {},
                modifier = Modifier
                    .size(56.dp)
                    .background(TypingPrimaryFixed, RoundedCornerShape(12.dp))
            ) {
                Icon(Icons.Default.VolumeUp, contentDescription = "Play audio", tint = TypingPrimary)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TagChip("Vocab")
            if (!word.pronunciation.isNullOrBlank()) {
                TagChip(word.pronunciation)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        val borderColor = when {
            isMatch -> TypingSecondary
            isWrong -> Color(0xFFBA1A1A)
            else -> Color.Transparent
        }
        val textColor = when {
            isMatch -> TypingSecondary
            isWrong -> Color(0xFFBA1A1A)
            else -> TypingPrimary
        }

        OutlinedTextField(
            value = userInput,
            onValueChange = onInputChange,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .shadow(4.dp, RoundedCornerShape(16.dp)),
            placeholder = {
                Text(
                    "Type the word...",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    color = TypingOutline
                )
            },
            textStyle = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp,
                textAlign = TextAlign.Center,
                color = textColor
            ),
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = TypingSurfaceContainerLow,
                focusedBorderColor = borderColor.takeIf { it != Color.Transparent } ?: TypingPrimary,
                unfocusedBorderColor = borderColor.takeIf { it != Color.Transparent } ?: Color.Transparent,
                cursorColor = TypingPrimary
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onSubmit() })
        )

        if (hintText != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Hint: $hintText",
                style = MaterialTheme.typography.bodyMedium,
                color = TypingPrimaryContainer,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onHint,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(2.dp, TypingOutlineVariant),
                modifier = Modifier.padding(horizontal = 6.dp)
            ) {
                Icon(Icons.Default.Lightbulb, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.size(6.dp))
                Text("Hint", fontWeight = FontWeight.SemiBold)
            }
            OutlinedButton(
                onClick = onSkip,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(2.dp, Color.Transparent),
                modifier = Modifier.padding(horizontal = 6.dp)
            ) {
                Icon(Icons.Default.FastForward, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.size(6.dp))
                Text("Skip", fontWeight = FontWeight.SemiBold, color = TypingOnSurfaceVariant)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Press ENTER to verify",
            style = MaterialTheme.typography.labelSmall,
            color = TypingOutline
        )
    }
}

@Composable
private fun TagChip(label: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = TypingSurfaceContainer
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall,
            color = TypingOnSurfaceVariant
        )
    }
}

@Composable
private fun CorrectFeedbackOverlay(onContinue: () -> Unit) {
    Surface(
        color = TypingSecondary,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = TypingSecondary)
                }
                Text(
                    text = "Correct!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = TypingSecondary
                )
            ) {
                Text("Continue", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun TypingFinishedScreen(
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
            color = TypingPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (total > 0) {
            Text(
                text = "$correct / $total từ đúng ($accuracy%)",
                style = MaterialTheme.typography.titleMedium,
                color = TypingSecondary
            )
        } else {
            Text(
                text = "Chưa có từ nào học hôm nay",
                style = MaterialTheme.typography.titleMedium,
                color = TypingOutline
            )
        }
        if (canReviewToday) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Có $todayWordsCount từ hôm nay để ôn lại",
                style = MaterialTheme.typography.bodyMedium,
                color = TypingOutline,
                textAlign = TextAlign.Center
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        if (canReviewToday) {
            Button(
                onClick = onReviewToday,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TypingPrimaryContainer)
            ) {
                Text("Ôn lại từ hôm nay")
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text("Quay về", color = TypingPrimary)
        }
    }
}
