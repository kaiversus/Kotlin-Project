package com.minlish.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minlish.app.data.repository.AuthRepository
import com.minlish.app.data.repository.LearningRepository
import com.minlish.app.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

data class LearnUiState(
    val isLoading: Boolean = false,
    val dailyTarget: Int = 10,
    val newWordsToday: Int = 0,
    val reviewToday: Int = 0,
    val progress: Float = 0f,
    val error: String? = null
)

class LearnViewModel : ViewModel() {
    private val authRepo = AuthRepository()
    private val userRepo = UserRepository()
    private val learningRepo = LearningRepository()

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
                val dailyTarget = user?.dailyTarget?.coerceAtLeast(1) ?: 10
                val (startOfDay, endOfDay) = todayRange()
                val (newToday, reviewToday) = learningRepo.getDailyPlanStats(userId, startOfDay, endOfDay)
                val completed = newToday + reviewToday
                val progress = (completed.toFloat() / dailyTarget.toFloat()).coerceIn(0f, 1f)

                _uiState.value = LearnUiState(
                    isLoading = false,
                    dailyTarget = dailyTarget,
                    newWordsToday = newToday,
                    reviewToday = reviewToday,
                    progress = progress
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.localizedMessage ?: "Không tải được kế hoạch hôm nay"
                )
            }
        }
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
