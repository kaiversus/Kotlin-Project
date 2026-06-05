package com.minlish.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minlish.app.data.model.LearningRecord
import com.minlish.app.data.model.Word
import com.minlish.app.data.repository.AuthRepository
import com.minlish.app.data.repository.LearningRepository
import com.minlish.app.data.repository.VocabSetRepository
import com.minlish.app.data.repository.WordRepository
import java.util.Calendar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class FlashcardSessionMode {
    SET,
    DAILY_PLAN;

    companion object {
        fun fromRoute(value: String): FlashcardSessionMode =
            if (value.equals("daily_plan", ignoreCase = true)) DAILY_PLAN else SET
    }
}

data class FlashcardState(
    val currentWord: Word? = null,
    val currentRecord: LearningRecord? = null,
    val setName: String = "",
    val isFlipped: Boolean = false,
    val progress: Int = 0,
    val total: Int = 0,
    val isFinished: Boolean = false,
    val isLoading: Boolean = true,
    val correctCount: Int = 0,
    val isTodayReviewSession: Boolean = false,
    val canReviewToday: Boolean = false,
    val todayWordsCount: Int = 0
)

class LearningViewModel : ViewModel() {
    private val wordRepo = WordRepository()
    private val setRepo = VocabSetRepository()
    private val learningRepo = LearningRepository()
    private val authRepo = AuthRepository()

    private val _state = MutableStateFlow(FlashcardState())
    val state: StateFlow<FlashcardState> = _state.asStateFlow()

    private var words = listOf<Word>()
    private var currentIndex = 0
    private var sessionCorrect = 0
    private var isTodayReviewSession = false
    private var sessionMode = FlashcardSessionMode.SET
    private var currentSetId = ""

    fun startSession(setId: String, mode: FlashcardSessionMode = FlashcardSessionMode.SET) {
        currentSetId = setId
        sessionMode = mode
        loadSession(todayReviewOnly = false)
    }

    fun startTodayReviewSession() {
        loadSession(todayReviewOnly = true)
    }

    private fun loadSession(todayReviewOnly: Boolean) {
        viewModelScope.launch {
            _state.value = FlashcardState(isLoading = true)
            try {
                val uid = authRepo.currentUser?.uid
                if (uid == null) {
                    _state.value = FlashcardState(isFinished = true, isLoading = false)
                    return@launch
                }

                val setName = when (sessionMode) {
                    FlashcardSessionMode.SET -> setRepo.getSet(currentSetId)?.name ?: "Bộ từ"
                    FlashcardSessionMode.DAILY_PLAN -> "Kế hoạch hôm nay"
                }
                val (startOfDay, endOfDay) = todayRange()

                when (sessionMode) {
                    FlashcardSessionMode.SET -> {
                        words = resolveSetSessionWords(
                            uid, currentSetId, startOfDay, endOfDay, todayReviewOnly
                        )
                        isTodayReviewSession = todayReviewOnly
                    }
                    FlashcardSessionMode.DAILY_PLAN -> {
                        val allWords = getAllUserWords(uid)
                        if (todayReviewOnly) {
                            val todayWords = learningRepo.getAllWordsStudiedToday(
                                uid, allWords, startOfDay, endOfDay
                            ).shuffled()
                            words = todayWords
                            isTodayReviewSession = true
                        } else {
                            val reviewWords = learningRepo.getDailyPlanReviewWords(
                                uid, allWords, endOfDay
                            ).shuffled()
                            words = reviewWords
                            isTodayReviewSession = false
                        }
                    }
                }

                if (words.isEmpty()) {
                    _state.value = buildFinishedState(
                        uid = uid,
                        startOfDay = startOfDay,
                        endOfDay = endOfDay,
                        setName = setName,
                        total = 0,
                        correct = 0
                    )
                    return@launch
                }
                currentIndex = 0
                sessionCorrect = 0
                showCard(setName)
            } catch (_: Exception) {
                _state.value = FlashcardState(isFinished = true, isLoading = false)
            }
        }
    }

    private suspend fun resolveSetSessionWords(
        uid: String,
        setId: String,
        startOfDay: Long,
        endOfDay: Long,
        todayReviewOnly: Boolean
    ): List<Word> {
        val setWords = wordRepo.getSetWords(setId)
        return if (todayReviewOnly) {
            learningRepo.getAllWordsStudiedToday(uid, setWords, startOfDay, endOfDay).shuffled()
        } else {
            setWords.shuffled()
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
                finishSession()
                updateSetWordCounts()
            } else {
                showCard(_state.value.setName)
            }
        }
    }

    fun previewIntervalLabel(grade: Int): String {
        val record = _state.value.currentRecord ?: return ""
        return learningRepo.formatInterval(learningRepo.previewGrade(record, grade).interval)
    }

    private suspend fun updateSetWordCounts() {
        val setId = when (sessionMode) {
            FlashcardSessionMode.SET -> currentSetId
            FlashcardSessionMode.DAILY_PLAN -> words.firstOrNull()?.vocabSetId
        } ?: return
        val uid = authRepo.currentUser?.uid ?: return
        try {
            val allSetWords = wordRepo.getSetWords(setId)
            val records = learningRepo.getAllRecords(uid)
            val recordMap = records.associateBy { it.wordId }
            val learnedWordsCount = allSetWords.count { word ->
                val rec = recordMap[word.id]
                rec != null && rec.status != "NEW"
            }
            setRepo.updateWordCount(setId, allSetWords.size.toLong(), learnedWordsCount.toLong())
        } catch (_: Exception) {
        }
    }

    private suspend fun finishSession() {
        val uid = authRepo.currentUser?.uid
        val setName = _state.value.setName
        if (uid == null) {
            _state.value = FlashcardState(isFinished = true, progress = words.size, correctCount = sessionCorrect)
            return
        }
        val (startOfDay, endOfDay) = todayRange()
        _state.value = buildFinishedState(
            uid = uid,
            startOfDay = startOfDay,
            endOfDay = endOfDay,
            setName = setName,
            total = words.size,
            correct = sessionCorrect
        )
    }

    private suspend fun buildFinishedState(
        uid: String,
        startOfDay: Long,
        endOfDay: Long,
        setName: String,
        total: Int,
        correct: Int
    ): FlashcardState {
        val scopeWords = when (sessionMode) {
            FlashcardSessionMode.SET -> wordRepo.getSetWords(currentSetId)
            FlashcardSessionMode.DAILY_PLAN -> getAllUserWords(uid)
        }
        val todayWords = learningRepo.getAllWordsStudiedToday(uid, scopeWords, startOfDay, endOfDay)
        return FlashcardState(
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

    private suspend fun showCard(setName: String) {
        val word = words[currentIndex]
        val uid = authRepo.currentUser?.uid ?: return
        val record = learningRepo.getOrCreateRecord(uid, word.id)
        _state.value = FlashcardState(
            currentWord = word,
            currentRecord = record,
            setName = setName,
            isFlipped = false,
            progress = currentIndex,
            total = words.size,
            isLoading = false,
            correctCount = sessionCorrect,
            isTodayReviewSession = isTodayReviewSession
        )
    }

    fun reset() {
        _state.value = FlashcardState()
        words = emptyList()
        currentIndex = 0
        sessionCorrect = 0
        isTodayReviewSession = false
        sessionMode = FlashcardSessionMode.SET
        currentSetId = ""
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
