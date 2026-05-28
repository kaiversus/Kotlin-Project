package com.minlish.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.minlish.app.ui.auth.LoginScreen
import com.minlish.app.ui.auth.RegisterScreen
import com.minlish.app.ui.home.HomeScreen
import com.minlish.app.ui.vocab.AddWordScreen
import com.minlish.app.ui.vocab.CreateSetScreen
import com.minlish.app.ui.vocab.FlashcardScreen
import com.minlish.app.ui.vocab.VocabSetListScreen
import com.minlish.app.ui.vocab.WordListScreen
import com.minlish.app.viewmodel.AuthViewModel
import com.minlish.app.viewmodel.LearningViewModel
import com.minlish.app.viewmodel.VocabViewModel
import com.minlish.app.viewmodel.WordViewModel
import java.net.URLDecoder
import java.net.URLEncoder

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    val HOME = Screen.Home.route
    val VOCAB_SETS = Screen.Vocab.route
    val LEARN = Screen.Learn.route
    val STATS = Screen.Stats.route
    val PROFILE = Screen.Profile.route
    const val CREATE_SET = "create_set"
    const val WORD_LIST = "word_list/{setId}/{setName}"
    const val ADD_WORD = "add_word/{setId}"
    const val FLASHCARD = "flashcard/{setId}"

    fun wordList(setId: String, setName: String) =
        "word_list/$setId/${URLEncoder.encode(setName, "UTF-8")}"
    fun addWord(setId: String) = "add_word/$setId"
    fun flashcard(setId: String) = "flashcard/$setId"
}

@Composable
fun MinLishNavGraph(isLoggedIn: Boolean, authViewModel: AuthViewModel) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val startDest = if (isLoggedIn) Routes.HOME else Routes.LOGIN
    val mainRoutes = setOf(Routes.HOME, Routes.VOCAB_SETS, Routes.LEARN, Routes.STATS, Routes.PROFILE)

    val vocabViewModel: VocabViewModel = viewModel()
    val wordViewModel: WordViewModel = viewModel()
    val learningViewModel: LearningViewModel = viewModel()

    Scaffold(
        bottomBar = {
            if (isLoggedIn && currentRoute in mainRoutes) {
                BottomBar(navController = navController)
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDest,
            modifier = Modifier.fillMaxSize()
        ) {

            composable(Routes.LOGIN) {
                LoginScreen(
                    authViewModel = authViewModel,
                    onNavigateToRegister = { navController.navigate(Routes.REGISTER) },
                    onLoginSuccess = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.REGISTER) {
                RegisterScreen(
                    authViewModel = authViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onRegisterSuccess = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.HOME) {
                HomeScreen(
                    authViewModel = authViewModel,
                    vocabViewModel = vocabViewModel,
                    onLogout = {
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(Routes.HOME) { inclusive = true }
                        }
                    },
                    onNavigateToSets = { navController.navigate(Routes.VOCAB_SETS) }
                )
            }

            composable(Routes.VOCAB_SETS) {
                VocabSetListScreen(
                    vocabViewModel = vocabViewModel,
                    onNavigateBack = { navController.navigate(Routes.HOME) },
                    onNavigateToWords = { setId, setName ->
                        navController.navigate(Routes.wordList(setId, setName))
                    },
                    onNavigateToCreate = { navController.navigate(Routes.CREATE_SET) }
                )
            }

            composable(Routes.LEARN) {
                VocabSetListScreen(
                    vocabViewModel = vocabViewModel,
                    onNavigateBack = { navController.navigate(Routes.HOME) },
                    onNavigateToWords = { setId, setName ->
                        navController.navigate(Routes.wordList(setId, setName))
                    },
                    onNavigateToCreate = { navController.navigate(Routes.CREATE_SET) }
                )
            }

            composable(Routes.STATS) {
                PlaceholderScreen(title = "Stats")
            }

            composable(Routes.PROFILE) {
                ProfileScreen(
                    displayName = authViewModel.displayName,
                    onLogout = {
                        authViewModel.logout()
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.CREATE_SET) {
                CreateSetScreen(
                    vocabViewModel = vocabViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Routes.WORD_LIST,
                arguments = listOf(
                    navArgument("setId") { type = NavType.StringType },
                    navArgument("setName") { type = NavType.StringType }
                )
            ) { backStack ->
                val setId = backStack.arguments?.getString("setId") ?: ""
                val setName = URLDecoder.decode(backStack.arguments?.getString("setName") ?: "", "UTF-8")
                WordListScreen(
                    setId = setId,
                    setName = setName,
                    wordViewModel = wordViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToAddWord = { id -> navController.navigate(Routes.addWord(id)) },
                    onNavigateToFlashcard = { id -> navController.navigate(Routes.flashcard(id)) }
                )
            }

            composable(
                route = Routes.ADD_WORD,
                arguments = listOf(navArgument("setId") { type = NavType.StringType })
            ) { backStack ->
                val setId = backStack.arguments?.getString("setId") ?: ""
                AddWordScreen(
                    setId = setId,
                    wordViewModel = wordViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Routes.FLASHCARD,
                arguments = listOf(navArgument("setId") { type = NavType.StringType })
            ) { backStack ->
                val setId = backStack.arguments?.getString("setId") ?: ""
                FlashcardScreen(
                    setId = setId,
                    learningViewModel = learningViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
private fun PlaceholderScreen(title: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "$title screen is coming soon")
    }
}

@Composable
private fun ProfileScreen(
    displayName: String,
    onLogout: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "Profile: $displayName")
    }
}
