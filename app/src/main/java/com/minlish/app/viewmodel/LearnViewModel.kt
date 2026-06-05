package com.minlish.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minlish.app.data.model.VocabSet
import com.minlish.app.data.model.Word
import com.minlish.app.data.repository.AuthRepository
import com.minlish.app.data.repository.LearningRepository
import com.minlish.app.data.repository.UserRepository
import com.minlish.app.data.repository.VocabSetRepository
import com.minlish.app.data.repository.WordRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

enum class DailyPlanListType {
    NEW, REVIEW
}

data class LearnUiState(
    val isLoading: Boolean = false,
    val dailyTarget: Int = 10,
    val completedToday: Int = 0,
    val plannedNewCount: Int = 0,
    val plannedReviewCount: Int = 0,
    val progress: Float = 0f,
    val error: String? = null,
    val activeList: DailyPlanListType? = null,
    val listWords: List<Word> = emptyList(),
    val listLoading: Boolean = false,
    val plannedNewWords: List<Word> = emptyList(),
    val plannedReviewWords: List<Word> = emptyList(),
    val completedNewWords: List<Word> = emptyList(),
    val completedReviewWords: List<Word> = emptyList()
)

class LearnViewModel : ViewModel() {
    private val authRepo = AuthRepository()
    private val userRepo = UserRepository()
    private val learningRepo = LearningRepository()
    private val setRepo = VocabSetRepository()
    private val wordRepo = WordRepository()

    private val _uiState = MutableStateFlow(LearnUiState(isLoading = true))
    val uiState: StateFlow<LearnUiState> = _uiState.asStateFlow()

    init {
        loadDailyPlan()
    }

    fun loadDailyPlan() {
        val userId = authRepo.currentUser?.uid ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val user = userRepo.getUser(userId)
                val dailyTarget = user?.dailyTarget?.toInt()?.coerceAtLeast(1) ?: 10
                val (startOfDay, endOfDay) = todayRange()
                val allWords = getAllUserWords(userId)

                val balanced = learningRepo.getBalancedDailyPlan(
                    userId, allWords, dailyTarget, startOfDay, endOfDay
                )
                val completedNewWords = learningRepo.getCompletedNewWordsToday(
                    userId, allWords, startOfDay, endOfDay
                )
                val completedReviewWords = learningRepo.getCompletedReviewWordsToday(
                    userId, allWords, startOfDay, endOfDay
                )
                val completedToday = completedNewWords.size + completedReviewWords.size
                val progress = (completedToday.toFloat() / dailyTarget.toFloat()).coerceIn(0f, 1f)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    dailyTarget = dailyTarget,
                    completedToday = completedToday,
                    plannedNewCount = balanced.plannedNew.size,
                    plannedReviewCount = balanced.plannedReview.size,
                    progress = progress,
                    plannedNewWords = balanced.plannedNew,
                    plannedReviewWords = balanced.plannedReview,
                    completedNewWords = completedNewWords,
                    completedReviewWords = completedReviewWords
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.localizedMessage ?: "Không tải được kế hoạch hôm nay"
                )
            }
        }
    }

    fun showDailyPlanList(type: DailyPlanListType) {
        val state = _uiState.value
        val words = when (type) {
            DailyPlanListType.NEW -> state.plannedNewWords
            DailyPlanListType.REVIEW -> state.plannedReviewWords
        }
        _uiState.value = state.copy(
            activeList = type,
            listWords = words,
            listLoading = false
        )
    }

    fun dismissDailyPlanList() {
        _uiState.value = _uiState.value.copy(
            activeList = null,
            listWords = emptyList(),
            listLoading = false
        )
    }

    suspend fun getFlashcardTarget(): VocabSet? {
        val userId = authRepo.currentUser?.uid ?: return null
        val sets = setRepo.getUserSets(userId)
        return sets.firstOrNull { it.totalWords > 0L } ?: sets.firstOrNull()
    }

    private suspend fun getAllUserWords(userId: String): List<Word> {
        val sets = setRepo.getUserSets(userId)
        return sets.flatMap { set -> wordRepo.getSetWords(set.id) }
    }

    private fun todayRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        cal.add(Calendar.DAY_OF_MONTH, 1)
        val end = cal.timeInMillis - 1
        return Pair(start, end)
    }
}
