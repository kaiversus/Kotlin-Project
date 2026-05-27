package com.minlish.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minlish.app.data.model.Word
import com.minlish.app.data.repository.VocabSetRepository
import com.minlish.app.data.repository.WordRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class WordUiState(
    val words: List<Word> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class WordViewModel : ViewModel() {
    private val wordRepo = WordRepository()
    private val setRepo = VocabSetRepository()

    private val _uiState = MutableStateFlow(WordUiState())
    val uiState: StateFlow<WordUiState> = _uiState.asStateFlow()

    private var currentSetId = ""

    fun loadWords(setId: String) {
        currentSetId = setId
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val words = wordRepo.getSetWords(setId)
                _uiState.value = WordUiState(words = words)
            } catch (e: Exception) {
                _uiState.value = WordUiState(error = e.localizedMessage)
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
                val updatedWords = wordRepo.getSetWords(setId)
                setRepo.updateWordCount(setId, updatedWords.size, 0)
                _uiState.value = WordUiState(words = updatedWords)
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
                val updatedWords = wordRepo.getSetWords(currentSetId)
                setRepo.updateWordCount(currentSetId, updatedWords.size, 0)
                _uiState.value = WordUiState(words = updatedWords)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.localizedMessage)
            }
        }
    }
}
