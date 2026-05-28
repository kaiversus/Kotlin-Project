package com.minlish.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen (
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Home: Screen(
        "home",
        "Home",
        Icons.Default.Home
    )

    object Vocab: Screen(
        "vocab",
        "Vocab",
        Icons.Default.Book
    )

    object Learn: Screen(
        "learn",
        "Learn",
        Icons.Default.School
    )

    object Stats : Screen(
        "stats",
        "Stats",
        Icons.Default.BarChart
    )

    object Profile : Screen(
        "profile",
        "Profile",
        Icons.Default.Person
    )
}
