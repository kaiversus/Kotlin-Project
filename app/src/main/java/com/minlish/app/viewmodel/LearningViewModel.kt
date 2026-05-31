package com.minlish.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minlish.app.data.model.LearningRecord
import com.minlish.app.data.model.Word
import com.minlish.app.data.repository.AuthRepository
import com.minlish.app.data.repository.LearningRepository
import com.minlish.app.data.repository.VocabSetRepository
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
    private val setRepo = VocabSetRepository()

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
        viewModelScope.launch {
            if (grade >= 2) sessionCorrect++
            learningRepo.updateWithGrade(record, grade)
            currentIndex++
            if (currentIndex >= words.size) {
                _state.value = _state.value.copy(
                    isFinished = true,
                    progress = words.size,
                    correctCount = sessionCorrect
                )
                // Cập nhật số lượng từ đã học của bộ từ vựng trên Firestore khi học xong
                val setId = words.firstOrNull()?.vocabSetId
                val uid = authRepo.currentUser?.uid ?: ""
                if (setId != null && uid.isNotEmpty()) {
                    try {
                        val allSetWords = wordRepo.getSetWords(setId)
                        val records = learningRepo.getAllRecords(uid)
                        val recordMap = records.associateBy { it.wordId }
                        val totalWordsCount = allSetWords.size
                        val learnedWordsCount = allSetWords.count { word ->
                            val rec = recordMap[word.id]
                            rec != null && rec.status != "NEW"
                        }
                        setRepo.updateWordCount(setId, totalWordsCount, learnedWordsCount)
                    } catch (e: Exception) {
                        // Fail silently to keep user experience smooth
                    }
                }
            } else {
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
