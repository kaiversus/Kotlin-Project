package com.minlish.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minlish.app.data.model.LearningRecord
import com.minlish.app.data.model.Word
import com.minlish.app.data.repository.AuthRepository
import com.minlish.app.data.repository.LearningRepository
import com.minlish.app.data.repository.WordRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FlashcardState(
    val currentWord: Word? = null,
    val currentRecord: LearningRecord? = null,
    val isFlipped: Boolean = false,
    val progress: Int = 0,
    val total: Int = 0,
    val isFinished: Boolean = false,
    val isLoading: Boolean = true,
    val correctCount: Int = 0
)

class LearningViewModel : ViewModel() {
    private val wordRepo = WordRepository()
    private val learningRepo = LearningRepository()
    private val authRepo = AuthRepository()

    private val _state = MutableStateFlow(FlashcardState())
    val state: StateFlow<FlashcardState> = _state.asStateFlow()

    private var words = listOf<Word>()
    private var currentIndex = 0
    private var sessionCorrect = 0

    fun startSession(setId: String) {
        viewModelScope.launch {
            _state.value = FlashcardState(isLoading = true)
            try {
                words = wordRepo.getSetWords(setId).shuffled()
                if (words.isEmpty()) {
                    _state.value = FlashcardState(isFinished = true, isLoading = false)
                    return@launch
                }
                currentIndex = 0
                sessionCorrect = 0
                showCard()
            } catch (e: Exception) {
                _state.value = FlashcardState(isFinished = true, isLoading = false)
            }
        }
    }

    fun flipCard() {
        _state.value = _state.value.copy(isFlipped = !_state.value.isFlipped)
    }

    fun grade(grade: Int) {
        val record = _state.value.currentRecord ?: return
        val uid = authRepo.currentUser?.uid ?: return
        viewModelScope.launch {
            val isNew = record.repetitions == 0
            if (grade >= 2) sessionCorrect++
            learningRepo.updateWithGrade(record, grade)
            currentIndex++
            if (currentIndex >= words.size) {
                // Session finished, update daily stats
                val newWords = if (isNew) 1 else 0
                val reviewedWords = if (isNew) 0 else 1
                try {
                    learningRepo.updateStatsAfterSession(
                        userId = uid,
                        newWords = newWords,
                        reviewedWords = reviewedWords,
                        correct = if (grade >= 2) 1 else 0,
                        total = 1
                    )
                } catch (e: Exception) {
                    // Ignore stats update errors to prevent crash
                }
                
                _state.value = _state.value.copy(
                    isFinished = true,
                    progress = words.size,
                    correctCount = sessionCorrect
                )
            } else {
                // If not finished, we still might want to update stats per card or at the end.
                // For simplicity, let's update per card for now or batch them.
                // Let's do per card for immediate feedback on Stats screen if user leaves early.
                val newWords = if (isNew) 1 else 0
                val reviewedWords = if (isNew) 0 else 1
                try {
                    learningRepo.updateStatsAfterSession(
                        userId = uid,
                        newWords = newWords,
                        reviewedWords = reviewedWords,
                        correct = if (grade >= 2) 1 else 0,
                        total = 1
                    )
                } catch (e: Exception) {
                    // Ignore
                }
                showCard()
            }
        }
    }

    private suspend fun showCard() {
        val word = words[currentIndex]
        val uid = authRepo.currentUser?.uid ?: return
        val record = learningRepo.getOrCreateRecord(uid, word.id)
        _state.value = FlashcardState(
            currentWord = word,
            currentRecord = record,
            isFlipped = false,
            progress = currentIndex,
            total = words.size,
            isLoading = false,
            correctCount = sessionCorrect
        )
    }

    fun reset() { _state.value = FlashcardState() }
}
