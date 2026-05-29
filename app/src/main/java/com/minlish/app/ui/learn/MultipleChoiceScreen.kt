package com.minlish.app.ui.learn

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minlish.app.viewmodel.QuizCheckResult
import com.minlish.app.viewmodel.QuizOption
import com.minlish.app.viewmodel.QuizViewModel

private val QuizPrimary = Color(0xFF3525CD)
private val QuizPrimaryContainer = Color(0xFF4F46E5)
private val QuizPrimaryFixed = Color(0xFFE2DFFF)
private val QuizSecondary = Color(0xFF006A61)
private val QuizTertiaryContainer = Color(0xFF885500)
private val QuizOnTertiaryContainer = Color(0xFFFFD4A4)
private val QuizSurface = Color(0xFFF8F9FF)
private val QuizSurfaceContainer = Color(0xFFE5EEFF)
private val QuizSurfaceContainerHighest = Color(0xFFD3E4FF)
private val QuizSurfaceContainerLow = Color(0xFFEFF4FF)
private val QuizSurfaceVariant = Color(0xFFD3E4FE)
private val QuizOnSurfaceVariant = Color(0xFF464555)
private val QuizOutlineVariant = Color(0xFFC7C4D8)
private val QuizError = Color(0xFFBA1A1A)

@Composable
fun MultipleChoiceScreen(
    setId: String,
    displayName: String,
    quizViewModel: QuizViewModel,
    onNavigateBack: () -> Unit
) {
    val state by quizViewModel.state.collectAsState()

    LaunchedEffect(setId) { quizViewModel.startSession(setId) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(QuizSurface)
    ) {
        QuizHeader(
            progress = state.progress,
            total = state.total,
            streak = state.streak,
            displayName = displayName,
            onClose = {
                quizViewModel.reset()
                onNavigateBack()
            }
        )

        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = QuizPrimary)
                }
            }
            state.isFinished -> {
                QuizFinishedScreen(
                    total = state.total,
                    correct = state.correctCount,
                    isTodayReviewSession = state.isTodayReviewSession,
                    canReviewToday = state.canReviewToday,
                    todayWordsCount = state.todayWordsCount,
                    onReviewToday = { quizViewModel.startTodayReviewSession(setId) },
                    onBack = {
                        quizViewModel.reset()
                        onNavigateBack()
                    }
                )
            }
            state.currentWord != null -> {
                QuizContent(
                    categoryLabel = state.categoryLabel,
                    questionText = state.questionText,
                    options = state.options,
                    selectedOptionIndex = state.selectedOptionIndex,
                    correctOptionIndex = state.correctOptionIndex,
                    checkResult = state.checkResult,
                    onSelectOption = quizViewModel::selectOption,
                    onCheck = quizViewModel::checkAnswer,
                    onSkip = quizViewModel::skipWord
                )
            }
        }
    }
}

@Composable
private fun QuizHeader(
    progress: Int,
    total: Int,
    streak: Int,
    displayName: String,
    onClose: () -> Unit
) {
    Surface(color = QuizSurface, shadowElevation = 2.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = QuizOnSurfaceVariant)
            }
            LinearProgressIndicator(
                progress = { if (total > 0) (progress + 1).toFloat() / total else 0f },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
                    .height(12.dp)
                    .clip(RoundedCornerShape(99.dp)),
                color = QuizSecondary,
                trackColor = QuizSurfaceContainerHighest
            )
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = QuizTertiaryContainer
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = QuizOnTertiaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = streak.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = QuizOnTertiaryContainer
                    )
                }
            }
            Spacer(modifier = Modifier.size(8.dp))
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(QuizPrimaryFixed),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = displayName.take(1).uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = QuizPrimary
                )
            }
        }
    }
}

@Composable
private fun QuizContent(
    categoryLabel: String,
    questionText: String,
    options: List<QuizOption>,
    selectedOptionIndex: Int?,
    correctOptionIndex: Int,
    checkResult: QuizCheckResult,
    onSelectOption: (Int) -> Unit,
    onCheck: () -> Unit,
    onSkip: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 24.dp, bottom = 120.dp)
        ) {
            Text(
                text = categoryLabel.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = QuizPrimary,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "What is the meaning of this word?",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0B1C30)
            )
            Spacer(modifier = Modifier.height(16.dp))

            DefinitionCard(text = questionText)

            Spacer(modifier = Modifier.height(24.dp))

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                options.forEachIndexed { index, option ->
                    QuizOptionCard(
                        option = option,
                        isSelected = selectedOptionIndex == index,
                        isCorrect = checkResult != QuizCheckResult.NONE && index == correctOptionIndex,
                        isWrong = checkResult == QuizCheckResult.WRONG && selectedOptionIndex == index,
                        enabled = checkResult != QuizCheckResult.CORRECT,
                        onClick = { onSelectOption(index) }
                    )
                }
            }
        }

        QuizFooter(
            modifier = Modifier.align(Alignment.BottomCenter),
            hasSelection = selectedOptionIndex != null,
            checkResult = checkResult,
            onCheck = onCheck,
            onSkip = onSkip
        )
    }
}

@Composable
private fun DefinitionCard(text: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(12.dp))
            .border(1.dp, QuizSurfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(160.dp)
                    .background(QuizPrimary)
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(24.dp)
                    .height(160.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "\"$text\"",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = Color(0xFF0B1C30),
                    lineHeight = 36.sp
                )
            }
        }
    }
}

@Composable
private fun QuizOptionCard(
    option: QuizOption,
    isSelected: Boolean,
    isCorrect: Boolean,
    isWrong: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val borderColor = when {
        isCorrect -> QuizSecondary
        isWrong -> QuizError
        isSelected -> QuizPrimary
        else -> Color.Transparent
    }
    val backgroundColor = when {
        isCorrect -> QuizPrimaryFixed.copy(alpha = 0.35f)
        isWrong -> Color(0xFFFFDAD6)
        isSelected -> QuizPrimaryFixed.copy(alpha = 0.2f)
        else -> Color.White
    }
    val labelBg = when {
        isCorrect || isSelected -> QuizPrimary
        else -> QuizSurfaceContainerHighest
    }
    val labelFg = when {
        isCorrect || isSelected -> Color.White
        else -> QuizOnSurfaceVariant
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(12.dp))
            .border(2.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(labelBg),
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
                    tint = QuizPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun QuizFooter(
    modifier: Modifier = Modifier,
    hasSelection: Boolean,
    checkResult: QuizCheckResult,
    onCheck: () -> Unit,
    onSkip: () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = QuizSurface,
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
                border = androidx.compose.foundation.BorderStroke(2.dp, QuizOutlineVariant),
                modifier = Modifier.height(52.dp)
            ) {
                Text("Skip", fontWeight = FontWeight.SemiBold, color = QuizOnSurfaceVariant)
            }

            val (buttonText, buttonColors, enabled) = when (checkResult) {
                QuizCheckResult.CORRECT -> Triple(
                    "Correct! Next",
                    ButtonDefaults.buttonColors(containerColor = QuizSecondary, contentColor = Color.White),
                    true
                )
                QuizCheckResult.WRONG -> Triple(
                    "Try Again",
                    ButtonDefaults.buttonColors(containerColor = QuizError, contentColor = Color.White),
                    true
                )
                QuizCheckResult.NONE -> if (hasSelection) {
                    Triple(
                        "Check Answer",
                        ButtonDefaults.buttonColors(containerColor = QuizPrimary, contentColor = Color.White),
                        true
                    )
                } else {
                    Triple(
                        "Check Answer",
                        ButtonDefaults.buttonColors(
                            containerColor = QuizOutlineVariant,
                            contentColor = QuizOnSurfaceVariant,
                            disabledContainerColor = QuizOutlineVariant,
                            disabledContentColor = QuizOnSurfaceVariant
                        ),
                        false
                    )
                }
            }

            Button(
                onClick = onCheck,
                enabled = enabled,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = buttonColors
            ) {
                Text(buttonText, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.size(8.dp))
                Icon(
                    imageVector = when (checkResult) {
                        QuizCheckResult.CORRECT -> Icons.Default.Check
                        QuizCheckResult.WRONG -> Icons.Default.Refresh
                        QuizCheckResult.NONE -> Icons.Default.ArrowForward
                    },
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun QuizFinishedScreen(
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
            color = QuizPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (total > 0) {
            Text(
                text = "$correct / $total câu đúng ($accuracy%)",
                style = MaterialTheme.typography.titleMedium,
                color = QuizSecondary
            )
        } else {
            Text(
                text = "Chưa có từ nào học hôm nay",
                style = MaterialTheme.typography.titleMedium,
                color = QuizOnSurfaceVariant
            )
        }
        if (canReviewToday) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Có $todayWordsCount từ hôm nay để ôn lại",
                style = MaterialTheme.typography.bodyMedium,
                color = QuizOnSurfaceVariant,
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
                colors = ButtonDefaults.buttonColors(containerColor = QuizPrimaryContainer)
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
            Text("Quay về", color = QuizPrimary)
        }
    }
}
