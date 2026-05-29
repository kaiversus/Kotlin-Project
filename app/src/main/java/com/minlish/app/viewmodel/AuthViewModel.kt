package com.minlish.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.minlish.app.data.model.User
import com.minlish.app.data.repository.AuthRepository
import com.minlish.app.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val errorMessage: String? = null
)

class AuthViewModel : ViewModel() {

    private val authRepo = AuthRepository()
    private val userRepo = UserRepository()

    val displayName: String
        get() = authRepo.currentUser?.displayName
            ?: authRepo.currentUser?.email?.substringBefore("@")
            ?: "Bạn"

    val currentUserId: String
        get() = authRepo.currentUser?.uid ?: ""

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        _uiState.value = AuthUiState(isLoggedIn = authRepo.isLoggedIn)
    }

    fun register(email: String, password: String, displayName: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val uid = authRepo.register(email, password)
                val user = User(
                    id = uid,
                    email = email,
                    displayName = displayName
                )
                userRepo.createUser(user)
                _uiState.value = AuthUiState(isLoggedIn = true)
            } catch (e: Exception) {
                _uiState.value = AuthUiState(
                    errorMessage = e.localizedMessage ?: "Đăng ký thất bại"
                )
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                authRepo.login(email, password)
                _uiState.value = AuthUiState(isLoggedIn = true)
            } catch (e: Exception) {
                _uiState.value = AuthUiState(
                    errorMessage = e.localizedMessage ?: "Đăng nhập thất bại"
                )
            }
        }
    }

    fun loginWithGoogle(idToken: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val uid = authRepo.loginWithGoogle(idToken)
                val existingUser = userRepo.getUser(uid)
                if (existingUser == null) {
                    val firebaseUser = authRepo.currentUser!!
                    val user = User(
                        id = uid,
                        email = firebaseUser.email ?: "",
                        displayName = firebaseUser.displayName ?: "User",
                        avatarUrl = firebaseUser.photoUrl?.toString()
                    )
                    userRepo.createUser(user)
                }
                _uiState.value = AuthUiState(isLoggedIn = true)
            } catch (e: Exception) {
                _uiState.value = AuthUiState(
                    errorMessage = e.localizedMessage ?: "Đăng nhập Google thất bại"
                )
            }
        }
    }

    fun logout() {
        authRepo.logout()
        _uiState.value = AuthUiState(isLoggedIn = false)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
