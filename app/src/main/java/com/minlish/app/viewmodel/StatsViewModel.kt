package com.minlish.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minlish.app.data.model.Achievement
import com.minlish.app.data.model.DailyStats
import com.minlish.app.data.model.Streak
import com.minlish.app.data.repository.LearningRepository
import com.minlish.app.data.repository.StatsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

data class StatsUiState(
    val totalWordsLearned: Int = 0,
    val masteredWords: Int = 0,
    val accuracy: Int = 0,
    val retentionRate: Int = 0,
    val dailyStats: List<DailyStats> = emptyList(),
    val dayLabels: List<String> = emptyList(),
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null
)

class StatsViewModel : ViewModel() {
    private val statsRepository = StatsRepository()
    private val learningRepository = LearningRepository()

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    private var streakJob: kotlinx.coroutines.Job? = null

    fun loadStats(userId: String) {
        if (userId.isBlank()) return

        // Real-time streak listener
        streakJob?.cancel()
        streakJob = statsRepository.getStreakFlow(userId)
            .onEach { streak ->
                _uiState.update { it.copy(
                    currentStreak = (streak?.currentStreak ?: 0L).toInt(),
                    longestStreak = (streak?.longestStreak ?: 0L).toInt()
                ) }
            }
            .catch { e ->
                _uiState.update { it.copy(error = e.message ?: "Failed to load streak") }
            }
            .launchIn(viewModelScope)

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val totalStats = learningRepository.getTotalStats(userId)
                
                // Fetch activity calculated from records instead of daily_stats collection
                val rawDailyStats = try { 
                    learningRepository.getWeeklyActivity(userId)
                } catch (e: Exception) { 
                    android.util.Log.e("StatsViewModel", "Error calculating weekly activity", e)
                    emptyList() 
                }
                
                val (paddedStats, labels) = prepareChartData(rawDailyStats)

                _uiState.update { it.copy(
                    totalWordsLearned = totalStats.first,
                    masteredWords = totalStats.second,
                    accuracy = totalStats.third,
                    retentionRate = calculateRetentionRate(totalStats.first, totalStats.second),
                    dailyStats = paddedStats,
                    dayLabels = labels,
                    isLoading = false
                ) }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isLoading = false,
                    error = e.message ?: "Unknown error"
                ) }
            }
        }
    }

    private fun prepareChartData(rawStats: List<DailyStats>): Pair<List<DailyStats>, List<String>> {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        val resultStats = mutableListOf<DailyStats>()
        val resultLabels = mutableListOf<String>()
        val days = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        
        // Create map for easy lookup, normalizing dates to midnight
        val statsMap = rawStats.associateBy { stats ->
            val cal = Calendar.getInstance().apply {
                timeInMillis = stats.date
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            cal.timeInMillis
        }
        
        // Go back 6 days from today
        calendar.add(Calendar.DAY_OF_YEAR, -6)
        
        for (i in 0..6) {
            val dateMillis = calendar.timeInMillis
            val stats = statsMap[dateMillis] ?: DailyStats(date = dateMillis)
            resultStats.add(stats)
            
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            resultLabels.add(days[dayOfWeek - 1])
            
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        
        return Pair(resultStats, resultLabels)
    }

    private fun calculateRetentionRate(total: Int, mastered: Int): Int {
        if (total == 0) return 0
        return (mastered * 100) / total
    }
}
