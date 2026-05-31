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

data class WordItemState(
    val word: Word,
    val record: LearningRecord? = null
)

data class WordUiState(
    val words: List<Word> = emptyList(),
    val wordItems: List<WordItemState> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class WordViewModel : ViewModel() {
    private val wordRepo = WordRepository()
    private val setRepo = VocabSetRepository()
    private val learningRepo = LearningRepository()
    private val authRepo = AuthRepository()

    private val _uiState = MutableStateFlow(WordUiState())
    val uiState: StateFlow<WordUiState> = _uiState.asStateFlow()

    private var currentSetId = ""

    fun loadWords(setId: String) {
        currentSetId = setId
        val uid = authRepo.currentUser?.uid ?: ""
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                // 1. Tải danh sách từ của bộ từ
                val words = wordRepo.getSetWords(setId)
                
                // 2. Tải bản ghi ôn tập (LearningRecords) để xác định trạng thái học tập
                val records = if (uid.isNotEmpty()) {
                    learningRepo.getAllRecords(uid)
                } else {
                    emptyList()
                }

                // 3. Ghép đôi Word và LearningRecord
                val recordMap = records.associateBy { it.wordId }
                val wordItems = words.map { word ->
                    WordItemState(
                        word = word,
                        record = recordMap[word.id]
                    )
                }

                // 4. Cập nhật số lượng từ đã thuộc của bộ từ vựng
                val totalWordsCount = words.size
                val learnedWordsCount = wordItems.count { it.record != null && it.record.status != "NEW" }
                setRepo.updateWordCount(setId, totalWordsCount, learnedWordsCount)

                _uiState.value = WordUiState(
                    words = words,
                    wordItems = wordItems,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = WordUiState(error = e.localizedMessage, isLoading = false)
            }
        }
    }

    fun addWord(
        setId: String,
        word: String, pronunciation: String, meaning: String,
        description: String, example: String,
        collocation: String, relatedWords: String, note: String,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val newWord = Word(
                    vocabSetId = setId,
                    word = word.trim(),
                    pronunciation = pronunciation.trim().takeIf { it.isNotBlank() },
                    meaning = meaning.trim(),
                    description = description.trim().takeIf { it.isNotBlank() },
                    exampleSentence = example.trim().takeIf { it.isNotBlank() },
                    collocation = collocation.trim().takeIf { it.isNotBlank() },
                    relatedWords = relatedWords.trim().takeIf { it.isNotBlank() },
                    note = note.trim().takeIf { it.isNotBlank() }
                )
                wordRepo.addWord(newWord)
                loadWords(setId)
                onDone()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.localizedMessage)
            }
        }
    }

    fun updateWordDetails(
        setId: String,
        wordId: String,
        word: String, pronunciation: String, meaning: String,
        description: String, example: String,
        collocation: String, relatedWords: String, note: String,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val updates = mapOf(
                    "word" to word.trim(),
                    "pronunciation" to pronunciation.trim(),
                    "meaning" to meaning.trim(),
                    "description" to description.trim(),
                    "exampleSentence" to example.trim(),
                    "collocation" to collocation.trim(),
                    "relatedWords" to relatedWords.trim(),
                    "note" to note.trim()
                )
                wordRepo.updateWord(wordId, updates)
                loadWords(setId)
                onDone()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.localizedMessage)
            }
        }
    }

    fun deleteWord(wordId: String) {
        viewModelScope.launch {
            try {
                wordRepo.deleteWord(wordId)
                loadWords(currentSetId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.localizedMessage)
            }
        }
    }
}
