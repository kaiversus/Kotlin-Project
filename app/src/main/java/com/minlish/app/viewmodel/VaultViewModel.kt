package com.minlish.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minlish.app.data.model.LearningRecord
import com.minlish.app.data.model.Word
import com.minlish.app.data.repository.VaultRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class VaultFilter {
    ALL, LEARNING, REVIEW, MASTERED
}

data class VaultUiState(
    val words: List<WordItemState> = emptyList(),
    val searchQuery: String = "",
    val selectedFilter: VaultFilter = VaultFilter.ALL,
    val isLoading: Boolean = false,
    val error: String? = null
)

class VaultViewModel : ViewModel() {
    private val vaultRepo = VaultRepository()

    private val _rawItems = MutableStateFlow<List<WordItemState>>(emptyList())
    private val _searchQuery = MutableStateFlow("")
    private val _selectedFilter = MutableStateFlow(VaultFilter.ALL)
    private val _isLoading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<VaultUiState> = combine(
        _rawItems, _searchQuery, _selectedFilter, _isLoading, _error
    ) { raw, query, filter, loading, err ->
        val filtered = raw.filter { item ->
            val matchesQuery = query.isEmpty() ||
                    item.word.word.contains(query, ignoreCase = true) ||
                    item.word.meaning.contains(query, ignoreCase = true)

            val matchesFilter = when (filter) {
                VaultFilter.ALL -> true
                VaultFilter.LEARNING -> item.record?.status == "LEARNING"
                VaultFilter.REVIEW -> item.record?.status == "REVIEW"
                VaultFilter.MASTERED -> item.record?.status == "MASTERED"
            }

            matchesQuery && matchesFilter
        }
        VaultUiState(
            words = filtered,
            searchQuery = query,
            selectedFilter = filter,
            isLoading = loading,
            error = err
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        VaultUiState(isLoading = true)
    )

    fun loadLearnedWords(userId: String) {
        if (userId.isBlank()) return
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val records = vaultRepo.getLearnedRecords(userId)
                if (records.isEmpty()) {
                    _rawItems.value = emptyList()
                    _isLoading.value = false
                    return@launch
                }

                val setIds = vaultRepo.getUserSets(userId)
                if (setIds.isEmpty()) {
                    _rawItems.value = emptyList()
                    _isLoading.value = false
                    return@launch
                }

                val words = vaultRepo.getWordsForSets(setIds)
                val recordMap = records.associateBy { it.wordId }

                val items = words.mapNotNull { word ->
                    val record = recordMap[word.id] ?: return@mapNotNull null
                    WordItemState(word = word, record = record)
                }.sortedBy { it.record?.nextReviewDate ?: 0L }

                _rawItems.value = items
            } catch (e: Exception) {
                _error.value = e.localizedMessage
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onFilterChange(filter: VaultFilter) {
        _selectedFilter.value = filter
    }
}
