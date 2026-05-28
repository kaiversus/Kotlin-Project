package com.minlish.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.minlish.app.navigation.MinLishNavGraph
import com.minlish.app.ui.theme.MinLishTheme
import com.minlish.app.viewmodel.AuthViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MinLishTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                     val authViewModel: AuthViewModel = viewModel()
                     val authState by authViewModel.uiState.collectAsState()

                     MinLishNavGraph(
                         isLoggedIn = authState.isLoggedIn,
                         authViewModel = authViewModel
                     )

//                    MinLishNavGraph(
//                        isLoggedIn = true,
//                        authViewModel = AuthViewModel()
//                    )
                }
            }
        }
    }
}
