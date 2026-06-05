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
    private val userRepo = UserRepository()
    private val learningRepo = LearningRepository()
    private val authRepo = AuthRepository()

    private val _state = MutableStateFlow(FlashcardState())
    val state: StateFlow<FlashcardState> = _state.asStateFlow()

    private var words = listOf<Word>()
    private var currentIndex = 0
    private var sessionCorrect = 0
    private var isTodayReviewSession = false

    fun startSession(setId: String) {
        loadSession(setId, todayReviewOnly = false)
    }

    fun startTodayReviewSession(setId: String) {
        loadSession(setId, todayReviewOnly = true)
    }

    private fun loadSession(setId: String, todayReviewOnly: Boolean) {
        viewModelScope.launch {
            _state.value = FlashcardState(isLoading = true)
            try {
                val uid = authRepo.currentUser?.uid
                if (uid == null) {
                    _state.value = FlashcardState(isFinished = true, isLoading = false)
                    return@launch
                }

                val set = setRepo.getSet(setId)
                val setName = set?.name ?: "Daily Plan"
                val dailyTarget = userRepo.getUser(uid)?.dailyTarget?.toInt()?.coerceAtLeast(1) ?: 10
                val (startOfDay, endOfDay) = todayRange()
                val allWords = if (set != null) {
                    wordRepo.getSetWords(setId)
                } else {
                    getAllUserWords(uid)
                }
                val session = learningRepo.resolveStudySessionWords(
                    uid, allWords, dailyTarget, startOfDay, endOfDay, todayReviewOnly
                )
                words = session.words
                isTodayReviewSession = session.isTodayReview

                if (words.isEmpty()) {
                    _state.value = buildFinishedState(
                        uid = uid,
                        allWords = allWords,
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

    fun flipCard() {
        _state.value = _state.value.copy(isFlipped = !_state.value.isFlipped)
    }

    fun grade(grade: Int) {
        val record = _state.value.currentRecord ?: return
        viewModelScope.launch {
            if (grade >= 2) sessionCorrect++
            try {
                learningRepo.updateWithGrade(record, grade)
            } catch (e: Exception) {
                // Prevent crash on database failure
            }
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
        val setId = words.firstOrNull()?.vocabSetId ?: return
        val uid = authRepo.currentUser?.uid ?: return
        try {
            val allSetWords = wordRepo.getSetWords(setId)
            val records = learningRepo.getAllRecords(uid)
            val recordMap = records.associateBy { it.wordId }
            val learnedWordsCount = allSetWords.count { word ->
                val rec = recordMap[word.id]
                rec != null && rec.status != "NEW"
            }.toLong()
            setRepo.updateWordCount(setId, allSetWords.size.toLong(), learnedWordsCount)
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
        val allWords = getAllUserWords(uid)
        _state.value = buildFinishedState(
            uid = uid,
            allWords = allWords,
            startOfDay = startOfDay,
            endOfDay = endOfDay,
            setName = setName,
            total = words.size,
            correct = sessionCorrect
        )
    }

    private suspend fun buildFinishedState(
        uid: String,
        allWords: List<Word>,
        startOfDay: Long,
        endOfDay: Long,
        setName: String,
        total: Int,
        correct: Int
    ): FlashcardState {
        val todayWords = learningRepo.getAllWordsStudiedToday(uid, allWords, startOfDay, endOfDay)
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
