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
                _uiState.value = VocabUiState(sets = sets)
            } catch (e: Exception) {
                _uiState.value = VocabUiState(error = e.localizedMessage)
            }
        }
    }

    fun createSet(name: String, description: String, onDone: () -> Unit) {
        val uid = currentUserId
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val set = VocabSet(userId = uid, name = name.trim(),
                    description = description.trim().takeIf { it.isNotBlank() })
                setRepo.createSet(set)
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
