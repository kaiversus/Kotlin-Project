package com.minlish.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minlish.app.data.model.LearningRecord
import com.minlish.app.data.model.Word
import com.minlish.app.data.repository.AuthRepository
import com.minlish.app.data.repository.LearningRepository
import com.minlish.app.data.repository.UserRepository
import com.minlish.app.data.repository.VocabSetRepository
import com.minlish.app.data.repository.WordRepository
import java.util.Calendar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class TypingFeedback {
    CORRECT
}

data class TypingState(
    val currentWord: Word? = null,
    val currentRecord: LearningRecord? = null,
    val setName: String = "",
    val userInput: String = "",
    val hintText: String? = null,
    val highlightedKey: Char? = null,
    val feedback: TypingFeedback? = null,
    val isWrong: Boolean = false,
    val progress: Int = 0,
    val total: Int = 0,
    val correctCount: Int = 0,
    val isLoading: Boolean = true,
    val isFinished: Boolean = false,
    val isTodayReviewSession: Boolean = false,
    val canReviewToday: Boolean = false,
    val todayWordsCount: Int = 0
)

class TypingViewModel : ViewModel() {
    private val wordRepo = WordRepository()
    private val setRepo = VocabSetRepository()
    private val userRepo = UserRepository()
    private val learningRepo = LearningRepository()
    private val authRepo = AuthRepository()

    private val _state = MutableStateFlow(TypingState())
    val state: StateFlow<TypingState> = _state.asStateFlow()

    private var words = listOf<Word>()
    private var currentIndex = 0
    private var sessionCorrect = 0
    private var hintLevel = 0
    private var isTodayReviewSession = false

    fun startSession(setId: String) {
        loadSession(setId, todayReviewOnly = false)
    }

    fun startTodayReviewSession(setId: String) {
        loadSession(setId, todayReviewOnly = true)
    }

    private fun loadSession(setId: String, todayReviewOnly: Boolean) {
        viewModelScope.launch {
            _state.value = TypingState(isLoading = true)
            try {
                val uid = authRepo.currentUser?.uid
                if (uid == null) {
                    _state.value = TypingState(isFinished = true, isLoading = false)
                    return@launch
                }

                val setName = setRepo.getSet(setId)?.name ?: "Daily Plan"
                val dailyTarget = userRepo.getUser(uid)?.dailyTarget?.coerceAtLeast(1) ?: 10
                val (startOfDay, endOfDay) = todayRange()
                val allWords = getAllUserWords(uid)
                val session = learningRepo.resolveStudySessionWords(
                    uid, allWords, dailyTarget, startOfDay, endOfDay, todayReviewOnly
                )
                words = session.words
                isTodayReviewSession = session.isTodayReview

                if (words.isEmpty()) {
                    _state.value = buildFinishedState(uid, allWords, startOfDay, endOfDay, setName, 0, 0)
                    return@launch
                }
                currentIndex = 0
                sessionCorrect = 0
                showWord(setName)
            } catch (_: Exception) {
                _state.value = TypingState(isFinished = true, isLoading = false)
            }
        }
    }

    fun onInputChange(value: String) {
        if (_state.value.feedback != null) return
        val lastChar = value.lastOrNull()
        _state.value = _state.value.copy(
            userInput = value,
            highlightedKey = lastChar?.uppercaseChar(),
            isWrong = false
        )
    }

    fun appendKey(char: Char) {
        if (_state.value.feedback != null) return
        onInputChange(_state.value.userInput + char)
    }

    fun submitAnswer() {
        val word = _state.value.currentWord ?: return
        if (_state.value.feedback != null) return

        val input = _state.value.userInput.trim()
        if (input.isEmpty()) return

        if (input.equals(word.word, ignoreCase = true)) {
            sessionCorrect++
            _state.value = _state.value.copy(
                feedback = TypingFeedback.CORRECT,
                isWrong = false,
                correctCount = sessionCorrect
            )
        } else {
            _state.value = _state.value.copy(isWrong = true)
        }
    }

    fun showHint() {
        val word = _state.value.currentWord ?: return
        if (_state.value.feedback != null) return

        hintLevel = (hintLevel + 1).coerceAtMost(word.word.length)
        val revealed = word.word.take(hintLevel)
        val hidden = "_".repeat((word.word.length - hintLevel).coerceAtLeast(0))
        _state.value = _state.value.copy(hintText = revealed + hidden)
    }

    fun skipWord() {
        if (_state.value.feedback != null) return
        viewModelScope.launch { moveToNext() }
    }

    fun continueAfterCorrect() {
        viewModelScope.launch {
            val record = _state.value.currentRecord ?: return@launch
            learningRepo.updateWithGrade(record, 2)
            moveToNext()
        }
    }

    fun reset() {
        _state.value = TypingState()
        words = emptyList()
        currentIndex = 0
        sessionCorrect = 0
        hintLevel = 0
        isTodayReviewSession = false
    }

    private suspend fun moveToNext() {
        currentIndex++
        hintLevel = 0
        if (currentIndex >= words.size) {
            finishSession()
        } else {
            showWord(_state.value.setName)
        }
    }

    private suspend fun finishSession() {
        val uid = authRepo.currentUser?.uid
        val setName = _state.value.setName
        if (uid == null) {
            _state.value = _state.value.copy(
                isFinished = true,
                progress = words.size,
                total = words.size,
                correctCount = sessionCorrect,
                feedback = null,
                userInput = "",
                hintText = null,
                currentWord = null
            )
            return
        }
        val (startOfDay, endOfDay) = todayRange()
        val allWords = getAllUserWords(uid)
        _state.value = buildFinishedState(uid, allWords, startOfDay, endOfDay, setName, words.size, sessionCorrect)
    }

    private suspend fun buildFinishedState(
        uid: String,
        allWords: List<Word>,
        startOfDay: Long,
        endOfDay: Long,
        setName: String,
        total: Int,
        correct: Int
    ): TypingState {
        val todayWords = learningRepo.getAllWordsStudiedToday(uid, allWords, startOfDay, endOfDay)
        return TypingState(
            setName = setName,
            isFinished = true,
            isLoading = false,
            progress = total,
            total = total,
            correctCount = correct,
            isTodayReviewSession = isTodayReviewSession,
            canReviewToday = todayWords.isNotEmpty(),
            todayWordsCount = todayWords.size
        )
    }

    private suspend fun showWord(setName: String) {
        val word = words[currentIndex]
        val uid = authRepo.currentUser?.uid ?: return
        val record = learningRepo.getOrCreateRecord(uid, word.id)
        hintLevel = 0
        _state.value = TypingState(
            currentWord = word,
            currentRecord = record,
            setName = setName,
            progress = currentIndex,
            total = words.size,
            isLoading = false,
            correctCount = sessionCorrect,
            isTodayReviewSession = isTodayReviewSession
        )
    }

    private suspend fun getAllUserWords(userId: String): List<Word> {
        return setRepo.getUserSets(userId).flatMap { set -> wordRepo.getSetWords(set.id) }
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
