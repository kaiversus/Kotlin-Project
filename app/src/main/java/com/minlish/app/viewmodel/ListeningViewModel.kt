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

data class ListeningOption(
    val label: String,
    val word: Word
)

data class ListeningState(
    val currentWord: Word? = null,
    val currentRecord: LearningRecord? = null,
    val options: List<ListeningOption> = emptyList(),
    val correctOptionIndex: Int = -1,
    val selectedOptionIndex: Int? = null,
    val checkResult: QuizCheckResult = QuizCheckResult.NONE,
    val isPlaying: Boolean = false,
    val hintText: String? = null,
    val setName: String = "",
    val progress: Int = 0,
    val total: Int = 0,
    val correctCount: Int = 0,
    val isLoading: Boolean = true,
    val isFinished: Boolean = false,
    val isTodayReviewSession: Boolean = false,
    val canReviewToday: Boolean = false,
    val todayWordsCount: Int = 0
)

class ListeningViewModel : ViewModel() {
    private val wordRepo = WordRepository()
    private val setRepo = VocabSetRepository()
    private val userRepo = UserRepository()
    private val learningRepo = LearningRepository()
    private val authRepo = AuthRepository()

    private val _state = MutableStateFlow(ListeningState())
    val state: StateFlow<ListeningState> = _state.asStateFlow()

    private var words = listOf<Word>()
    private var distractorPool = listOf<Word>()
    private var currentIndex = 0
    private var sessionCorrect = 0
    private var isTodayReviewSession = false
    private var hasFailedCurrentWord = false

    fun startSession(setId: String) {
        loadSession(setId, todayReviewOnly = false)
    }

    fun startTodayReviewSession(setId: String) {
        loadSession(setId, todayReviewOnly = true)
    }

    private fun loadSession(setId: String, todayReviewOnly: Boolean) {
        viewModelScope.launch {
            _state.value = ListeningState(isLoading = true)
            try {
                val uid = authRepo.currentUser?.uid ?: run {
                    _state.value = ListeningState(isFinished = true, isLoading = false)
                    return@launch
                }

                val set = setRepo.getSet(setId)
                val setName = set?.name ?: "Daily Plan"
                val dailyTarget = userRepo.getUser(uid)?.dailyTarget?.toInt()?.coerceAtLeast(1) ?: 10
                val (startOfDay, endOfDay) = todayRange()
                val allWords = getAllUserWords(uid)
                val session = learningRepo.resolveStudySessionWords(
                    uid, allWords, dailyTarget, startOfDay, endOfDay, todayReviewOnly
                )
                words = session.words
                isTodayReviewSession = session.isTodayReview
                distractorPool = allWords

                if (words.isEmpty()) {
                    _state.value = buildFinishedState(uid, allWords, startOfDay, endOfDay, setName, 0, 0)
                    return@launch
                }
                currentIndex = 0
                sessionCorrect = 0
                showQuestion(setName)
            } catch (_: Exception) {
                _state.value = ListeningState(isFinished = true, isLoading = false)
            }
        }
    }

    fun playAudio() {
        if (_state.value.isPlaying || _state.value.currentWord == null) return
        _state.value = _state.value.copy(isPlaying = true)
    }

    fun onAudioFinished() {
        _state.value = _state.value.copy(isPlaying = false)
    }

    fun selectOption(index: Int) {
        if (_state.value.checkResult == QuizCheckResult.CORRECT) return
        _state.value = _state.value.copy(
            selectedOptionIndex = index,
            checkResult = QuizCheckResult.NONE
        )
    }

    fun checkAnswer() {
        val state = _state.value
        val selected = state.selectedOptionIndex ?: return

        when (state.checkResult) {
            QuizCheckResult.CORRECT -> {
                viewModelScope.launch {
                    try {
                        val record = state.currentRecord ?: return@launch
                        val grade = if (hasFailedCurrentWord) 0 else 2
                        learningRepo.updateWithGrade(record, grade)
                    } catch (e: Exception) {
                        // Prevent crash on database failure
                    }
                    moveToNext()
                }
            }
            QuizCheckResult.WRONG -> {
                _state.value = state.copy(
                    selectedOptionIndex = null,
                    checkResult = QuizCheckResult.NONE
                )
            }
            QuizCheckResult.NONE -> {
                if (selected == state.correctOptionIndex) {
                    sessionCorrect++
                    _state.value = state.copy(
                        checkResult = QuizCheckResult.CORRECT,
                        correctCount = sessionCorrect
                    )
                } else {
                    hasFailedCurrentWord = true
                    _state.value = state.copy(checkResult = QuizCheckResult.WRONG)
                }
            }
        }
    }

    fun showHint() {
        val word = _state.value.currentWord ?: return
        if (_state.value.checkResult == QuizCheckResult.CORRECT) return
        hasFailedCurrentWord = true
        val meaning = word.meaning
        _state.value = _state.value.copy(hintText = meaning)
    }

    fun skipWord() {
        if (_state.value.checkResult == QuizCheckResult.CORRECT) return
        viewModelScope.launch {
            moveToNext()
        }
    }

    fun reset() {
        _state.value = ListeningState()
        words = emptyList()
        distractorPool = emptyList()
        currentIndex = 0
        sessionCorrect = 0
        isTodayReviewSession = false
    }

    private suspend fun moveToNext() {
        currentIndex++
        if (currentIndex >= words.size) {
            finishSession()
        } else {
            showQuestion(_state.value.setName)
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
                currentWord = null,
                options = emptyList(),
                checkResult = QuizCheckResult.NONE,
                selectedOptionIndex = null,
                isPlaying = false,
                hintText = null
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
    ): ListeningState {
        val todayWords = learningRepo.getAllWordsStudiedToday(uid, allWords, startOfDay, endOfDay)
        return ListeningState(
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

    private suspend fun showQuestion(setName: String) {
        hasFailedCurrentWord = false
        val word = words[currentIndex]
        val uid = authRepo.currentUser?.uid ?: return
        val record = learningRepo.getOrCreateRecord(uid, word.id)
        val (optionList, correctIndex) = buildOptions(word)

        _state.value = ListeningState(
            currentWord = word,
            currentRecord = record,
            options = optionList,
            correctOptionIndex = correctIndex,
            setName = setName,
            progress = currentIndex,
            total = words.size,
            isLoading = false,
            correctCount = sessionCorrect,
            isTodayReviewSession = isTodayReviewSession
        )
    }

    private fun buildOptions(correctWord: Word): Pair<List<ListeningOption>, Int> {
        val labels = listOf("1", "2", "3", "4")
        val distractors = distractorPool
            .filter { it.id != correctWord.id }
            .shuffled()
            .take(3)

        val merged = (listOf(correctWord) + distractors).shuffled()
        val correctIndex = merged.indexOfFirst { it.id == correctWord.id }
        val options = merged.mapIndexed { index, word ->
            ListeningOption(label = labels.getOrElse(index) { "?" }, word = word)
        }
        return options to correctIndex
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
