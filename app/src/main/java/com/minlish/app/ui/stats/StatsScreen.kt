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
    val maxWords = dailyStats.maxOfOrNull { it.newWordsLearned + it.wordsReviewed }?.coerceAtLeast(10) ?: 10
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(modifier = Modifier.padding(16.dp)) {
        Canvas(modifier = Modifier
            .weight(1f)
            .fillMaxWidth()) {
            val barWidth = size.width / (dailyStats.size * 2f)
            val spaceBetween = size.width / (dailyStats.size * 2f)
            
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
                val total = stats.newWordsLearned + stats.wordsReviewed
                val barHeight = (total.toFloat() / maxWords) * size.height
                
                val x = index * (barWidth + spaceBetween) + spaceBetween / 2
                val y = size.height - barHeight
                
                // Draw review words part
                val reviewHeight = (stats.wordsReviewed.toFloat() / maxWords) * size.height
                drawRect(
                    color = secondaryColor.copy(alpha = 0.7f),
                    topLeft = Offset(x, size.height - reviewHeight),
                    size = androidx.compose.ui.geometry.Size(barWidth, reviewHeight)
                )

                // Draw new words part on top
                val newWordsHeight = (stats.newWordsLearned.toFloat() / maxWords) * size.height
                drawRect(
                    color = primaryColor,
                    topLeft = Offset(x, y),
                    size = androidx.compose.ui.geometry.Size(barWidth, newWordsHeight)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            labels.forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = labelColor,
                    modifier = Modifier.width(32.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(8.dp).background(primaryColor, RoundedCornerShape(2.dp)))
            Spacer(Modifier.width(4.dp))
            Text("New", style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.width(16.dp))
            Box(Modifier.size(8.dp).background(secondaryColor.copy(alpha = 0.7f), RoundedCornerShape(2.dp)))
            Spacer(Modifier.width(4.dp))
            Text("Review", style = MaterialTheme.typography.labelSmall)
        }
    }
}
