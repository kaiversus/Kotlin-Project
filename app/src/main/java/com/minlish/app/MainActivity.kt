package com.minlish.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.minlish.app.navigation.MinLishNavGraph
import com.minlish.app.ui.theme.MinLishTheme
import com.minlish.app.viewmodel.AuthViewModel
import com.minlish.app.viewmodel.ProfileViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val authViewModel: AuthViewModel = viewModel()
            val profileViewModel: ProfileViewModel = viewModel()
            
            val authState by authViewModel.uiState.collectAsState()
            val profileState by profileViewModel.uiState.collectAsState()

            // Observe user profile when logged in to get dark mode preference
            LaunchedEffect(authState.isLoggedIn) {
                if (authState.isLoggedIn) {
                    profileViewModel.loadUserProfile(authViewModel.currentUserId)
                }
            }

            val isDarkMode = profileState.user?.darkMode ?: isSystemInDarkTheme()

            MinLishTheme(darkTheme = isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                     MinLishNavGraph(
                         isLoggedIn = authState.isLoggedIn,
                         authViewModel = authViewModel
                     )
                }
            }
        }
    }
}
