package com.minlish.app.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.minlish.app.data.model.Achievement
import com.minlish.app.data.model.DailyStats
import com.minlish.app.viewmodel.StatsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    userId: String,
    viewModel: StatsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(userId) {
        viewModel.loadStats(userId)
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Progress") })
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                item {
                    OverviewSection(
                        totalWords = uiState.totalWordsLearned,
                        masteredWords = uiState.masteredWords,
                        accuracy = uiState.accuracy,
                        retention = uiState.retentionRate
                    )
                }

                item {
                    StreakSection(
                        currentStreak = uiState.currentStreak,
                        longestStreak = uiState.longestStreak
                    )
                }

                item {
                    ActivityChartSection(
                        dailyStats = uiState.dailyStats,
                        labels = uiState.dayLabels
                    )
                }
            }
        }
    }
}

@Composable
fun OverviewSection(totalWords: Int, masteredWords: Int, accuracy: Int, retention: Int) {
    Column {
        Text("Overview", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard("Total Words", totalWords.toString(), Modifier.weight(1f), MaterialTheme.colorScheme.primaryContainer)
            StatCard("Mastered", masteredWords.toString(), Modifier.weight(1f), MaterialTheme.colorScheme.secondaryContainer)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard("Accuracy", "$accuracy%", Modifier.weight(1f), MaterialTheme.colorScheme.tertiaryContainer)
            StatCard("Retention", "$retention%", Modifier.weight(1f), MaterialTheme.colorScheme.surfaceVariant)
        }
    }
}

@Composable
fun StatCard(label: String, value: String, modifier: Modifier = Modifier, containerColor: Color) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun StreakSection(currentStreak: Int, longestStreak: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Ô Current Streak
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.LocalFireDepartment,
                    contentDescription = null,
                    tint = Color.Yellow,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text("Streak", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelMedium)
                Text(
                    "$currentStreak Days",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Ô Best Record
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    tint = Color.Yellow,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text("Best", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelMedium)
                Text(
                    "$longestStreak Days",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun ActivityChartSection(dailyStats: List<DailyStats>, labels: List<String>) {
    Column {
        Text("Weekly Activity", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (dailyStats.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No activity data yet")
                }
            } else {
                SimpleBarChart(dailyStats, labels)
            }
        }
    }
}

@Composable
fun SimpleBarChart(dailyStats: List<DailyStats>, labels: List<String>) {
    val maxWords = dailyStats.maxOfOrNull { maxOf(it.newWordsLearned, it.wordsReviewed) }?.coerceAtLeast(10) ?: 10
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(modifier = Modifier.padding(16.dp)) {
        Canvas(modifier = Modifier
            .weight(1f)
            .fillMaxWidth()) {
            val totalWidth = size.width
            val groupWidth = totalWidth / dailyStats.size
            val barWidth = groupWidth * 0.35f // Each bar takes 35% of group width
            val spacing = 2.dp.toPx() // Small space between side-by-side bars
            
            // Draw horizontal grid lines
            val gridLines = 4
            for (i in 0..gridLines) {
                val y = size.height - (i.toFloat() / gridLines) * size.height
                drawLine(
                    color = labelColor.copy(alpha = 0.1f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            dailyStats.forEachIndexed { index, stats ->
                val groupCenterX = index * groupWidth + groupWidth / 2
                
                // 1. New Words Bar (Left of center)
                val newWordsHeight = (stats.newWordsLearned.toFloat() / maxWords) * size.height
                val newWordsX = groupCenterX - barWidth - spacing / 2
                drawRect(
                    color = primaryColor,
                    topLeft = Offset(newWordsX, size.height - newWordsHeight),
                    size = androidx.compose.ui.geometry.Size(barWidth, newWordsHeight)
                )

                // 2. Review Words Bar (Right of center)
                val reviewWordsHeight = (stats.wordsReviewed.toFloat() / maxWords) * size.height
                val reviewWordsX = groupCenterX + spacing / 2
                drawRect(
                    color = secondaryColor,
                    topLeft = Offset(reviewWordsX, size.height - reviewWordsHeight),
                    size = androidx.compose.ui.geometry.Size(barWidth, reviewWordsHeight)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            labels.forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = labelColor,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Legend with explanation
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(12.dp).background(primaryColor, RoundedCornerShape(2.dp)))
                    Spacer(Modifier.width(6.dp))
                    Text("New words learned", style = MaterialTheme.typography.labelMedium)
                }
                Spacer(Modifier.width(24.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(12.dp).background(secondaryColor, RoundedCornerShape(2.dp)))
                    Spacer(Modifier.width(6.dp))
                    Text("Words reviewed", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
