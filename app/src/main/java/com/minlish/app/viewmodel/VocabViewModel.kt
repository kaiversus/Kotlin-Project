package com.minlish.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minlish.app.data.model.VocabSet
import com.minlish.app.data.repository.AuthRepository
import com.minlish.app.data.repository.VocabSetRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class VocabUiState(
    val sets: List<VocabSet> = emptyList(),
    val filteredSets: List<VocabSet> = emptyList(),
    val searchQuery: String = "",
    val selectedTag: String = "Tất cả",
    val showFavoritesOnly: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

class VocabViewModel : ViewModel() {
    private val setRepo = VocabSetRepository()
    private val authRepo = AuthRepository()

    private val _uiState = MutableStateFlow(VocabUiState())
    val uiState: StateFlow<VocabUiState> = _uiState.asStateFlow()

    val currentUserId: String get() = authRepo.currentUser?.uid ?: ""

    init { loadSets() }

    fun loadSets() {
        val uid = currentUserId
        if (uid.isEmpty()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val sets = setRepo.getUserSets(uid)
                _uiState.value = _uiState.value.copy(
                    sets = sets,
                    isLoading = false
                )
                applyFilters()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.localizedMessage)
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        applyFilters()
    }

    fun updateSelectedTag(tag: String) {
        _uiState.value = _uiState.value.copy(selectedTag = tag)
        applyFilters()
    }

    fun toggleShowFavoritesOnly(show: Boolean) {
        _uiState.value = _uiState.value.copy(showFavoritesOnly = show)
        applyFilters()
    }

    private fun applyFilters() {
        val state = _uiState.value
        var filtered = state.sets

        // 1. Lọc theo từ khóa tìm kiếm (tên hoặc mô tả)
        if (state.searchQuery.isNotBlank()) {
            val query = state.searchQuery.trim().lowercase()
            filtered = filtered.filter {
                it.name.lowercase().contains(query) ||
                (it.description?.lowercase()?.contains(query) == true)
            }
        }

        // 2. Lọc theo Tag
        if (state.selectedTag != "Tất cả") {
            val tagQuery = state.selectedTag.lowercase()
            filtered = filtered.filter { set ->
                set.tags.lowercase().contains(tagQuery)
            }
        }

        // 3. Lọc theo Favorites
        if (state.showFavoritesOnly) {
            filtered = filtered.filter { it.isFavorite }
        }

        _uiState.value = _uiState.value.copy(filteredSets = filtered)
    }

    fun toggleFavorite(setId: String, isFavorite: Boolean) {
        viewModelScope.launch {
            try {
                setRepo.updateFavoriteStatus(setId, isFavorite)
                loadSets()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.localizedMessage)
            }
        }
    }

    fun createSet(name: String, description: String, onDone: () -> Unit) {
        val uid = currentUserId
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                // Tự động phân tách tag dựa trên tên bộ từ để hiển thị bộ lọc thông minh
                val detectedTag = when {
                    name.lowercase().contains("ielts") -> "[\"IELTS\"]"
                    name.lowercase().contains("business") || name.lowercase().contains("workplace") || name.lowercase().contains("thương mại") -> "[\"Business\"]"
                    name.lowercase().contains("travel") || name.lowercase().contains("tourist") || name.lowercase().contains("du lịch") -> "[\"Travel\"]"
                    else -> "[]"
                }

                val set = VocabSet(
                    userId = uid,
                    name = name.trim(),
                    description = description.trim().takeIf { it.isNotBlank() },
                    tags = detectedTag
                )
                setRepo.createSet(set)
                loadSets()
                onDone()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.localizedMessage)
            }
        }
    }

    fun importCsvToSet(setName: String, setDescription: String, csvText: String, onDone: () -> Unit) {
        val uid = currentUserId
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                // 1. Parse CSV
                val lines = csvText.split("\n")
                val wordsToImport = mutableListOf<com.minlish.app.data.model.Word>()
                for (line in lines) {
                    val parts = line.split(",").map { it.trim() }
                    if (parts.isEmpty() || parts[0].isBlank()) continue

                    val wordText = parts[0]
                    val pronunciation = parts.getOrNull(1) ?: ""
                    val meaning = parts.getOrNull(2) ?: ""
                    val description = parts.getOrNull(3) ?: ""
                    val example = parts.getOrNull(4) ?: ""

                    wordsToImport.add(
                        com.minlish.app.data.model.Word(
                            word = wordText,
                            pronunciation = pronunciation.takeIf { it.isNotBlank() },
                            meaning = meaning,
                            description = description.takeIf { it.isNotBlank() },
                            exampleSentence = example.takeIf { it.isNotBlank() }
                        )
                    )
                }

                if (wordsToImport.isEmpty()) {
                    throw Exception("Không tìm thấy từ vựng hợp lệ trong nội dung CSV. Hãy đảm bảo phân tách bằng dấu phẩy.")
                }

                // 2. Tự động xác định Tag
                val detectedTag = when {
                    setName.lowercase().contains("ielts") -> "[\"IELTS\"]"
                    setName.lowercase().contains("business") || setName.lowercase().contains("workplace") || setName.lowercase().contains("thương mại") -> "[\"Business\"]"
                    setName.lowercase().contains("travel") || setName.lowercase().contains("tourist") || setName.lowercase().contains("du lịch") -> "[\"Travel\"]"
                    else -> "[]"
                }

                val newSet = VocabSet(
                    userId = uid,
                    name = setName.trim(),
                    description = setDescription.trim().takeIf { it.isNotBlank() },
                    tags = detectedTag,
                    totalWords = wordsToImport.size
                )
                val newSetId = setRepo.createSet(newSet)

                // 3. Thêm các từ vựng vào bộ từ
                val wordRepo = com.minlish.app.data.repository.WordRepository()
                for (w in wordsToImport) {
                    wordRepo.addWord(w.copy(vocabSetId = newSetId))
                }

                loadSets()
                onDone()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.localizedMessage)
            }
        }
    }

    fun deleteSet(setId: String) {
        viewModelScope.launch {
            try {
                setRepo.deleteSet(setId)
                loadSets()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.localizedMessage)
            }
        }
    }

    fun clearError() { _uiState.value = _uiState.value.copy(error = null) }
}
