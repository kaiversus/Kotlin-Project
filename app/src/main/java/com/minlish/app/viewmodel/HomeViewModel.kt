package com.minlish.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minlish.app.data.model.User
import com.minlish.app.data.model.Word
import com.minlish.app.data.repository.AuthRepository
import com.minlish.app.data.repository.UserRepository
import com.minlish.app.data.repository.LearningRepository
import com.minlish.app.data.repository.WordRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

data class HomeUiState(
    val isLoading: Boolean = true,
    val user: User? = null,
    val greeting: String = "",
    val wordsLearnedToday: Int = 0,
    val dailyTarget: Int = 10,
    val dueWordsCount: Int = 0,
    val currentStreak: Int = 0,
    val averageAccuracy: Int = 0,
    val totalLearnedWords: Int = 0,
    val dueWordsList: List<Word> = emptyList(),
    val errorMessage: String? = null
)

class HomeViewModel : ViewModel() {
    private val authRepo = AuthRepository()
    private val userRepo = UserRepository()
    private val learningRepo = LearningRepository()
    private val wordRepo = WordRepository()

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun loadDashboardData() {
        val uid = authRepo.currentUser?.uid
        if (uid == null) {
            _uiState.value = HomeUiState(
                isLoading = false,
                errorMessage = "Người dùng chưa đăng nhập"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                // Tải song song các nguồn dữ liệu để tối ưu tốc độ phản hồi
                val userDeferred = async { userRepo.getUser(uid) }
                val streakDeferred = async { learningRepo.getStreak(uid) }
                val dailyStatsDeferred = async { learningRepo.getDailyStats(uid) }
                val dueRecordsDeferred = async { learningRepo.getDueRecords(uid, limit = 5) }
                val dueRecordsCountDeferred = async { learningRepo.getDueRecordsCount(uid) }
                val totalStatsDeferred = async { learningRepo.getTotalStats(uid) }

                val user = userDeferred.await()
                val streak = streakDeferred.await()
                val dailyStats = dailyStatsDeferred.await()
                val dueRecords = dueRecordsDeferred.await()
                val dueRecordsCount = dueRecordsCountDeferred.await()
                val totalStats = totalStatsDeferred.await()

                // Từ danh sách records đến hạn ôn, lấy thông tin chi tiết các Word
                val dueWordIds = dueRecords.map { it.wordId }
                val dueWords = if (dueWordIds.isNotEmpty()) {
                    wordRepo.getWordsByIds(dueWordIds)
                } else {
                    emptyList()
                }

                val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                val greeting = getGreetingMessage(hour)

                _uiState.value = HomeUiState(
                    isLoading = false,
                    user = user,
                    greeting = greeting,
                    wordsLearnedToday = dailyStats.newWordsLearned + dailyStats.wordsReviewed,
                    dailyTarget = user?.dailyTarget ?: 10,
                    dueWordsCount = dueRecordsCount,
                    currentStreak = streak.currentStreak,
                    averageAccuracy = totalStats.third,
                    totalLearnedWords = totalStats.first,
                    dueWordsList = dueWords
                )
            } catch (e: Exception) {
                _uiState.value = HomeUiState(
                    isLoading = false,
                    errorMessage = "Lỗi khi nạp dữ liệu: ${e.localizedMessage}"
                )
            }
        }
    }

    private fun getGreetingMessage(hour: Int): String {
        return when (hour) {
            in 5..11 -> "Good Morning"
            in 12..17 -> "Good Afternoon"
            else -> "Good Evening"
        }
    }
}
