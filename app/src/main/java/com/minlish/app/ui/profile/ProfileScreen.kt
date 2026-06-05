package com.minlish.app.ui.profile

import android.app.TimePickerDialog
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.minlish.app.R
import com.minlish.app.viewmodel.ProfileViewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userId: String,
    onLogout: () -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showTargetDialog by remember { mutableStateOf(false) }

    LaunchedEffect(userId) {
        viewModel.loadUserProfile(userId)
    }

    if (showTargetDialog) {
        DailyTargetDialog(
            initialValue = (uiState.user?.dailyTarget ?: 10L).toInt(),
            onDismiss = { showTargetDialog = false },
            onConfirm = { newTarget ->
                viewModel.updateUserProfile(userId, mapOf("dailyTarget" to newTarget.toLong()))
                showTargetDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Profile") })
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item {
                    UserInfoSection(
                        name = uiState.user?.displayName ?: "User",
                        email = uiState.user?.email ?: "",
                        level = uiState.user?.englishLevel ?: "A1"
                    )
                }

                item {
                    LearningGoalSection(
                        currentGoal = uiState.user?.learningGoal ?: "General",
                        onGoalChange = { newGoal ->
                            viewModel.updateUserProfile(userId, mapOf("learningGoal" to newGoal))
                        }
                    )
                }

                item {
                    SettingsSection(
                        dailyTarget = (uiState.user?.dailyTarget ?: 10L).toInt(),
                        isDarkMode = uiState.user?.darkMode ?: false,
                        notificationTime = uiState.user?.notificationTime ?: "20:00",
                        onDailyTargetClick = { showTargetDialog = true },
                        onDarkModeToggle = { enabled ->
                            viewModel.updateUserProfile(userId, mapOf("darkMode" to enabled))
                        },
                        onNotificationTimeClick = {
                            val timeParts = (uiState.user?.notificationTime ?: "20:00").split(":")
                            val hour = timeParts[0].toInt()
                            val minute = timeParts[1].toInt()
                            TimePickerDialog(context, { _, h, m ->
                                val newTime = String.format("%02d:%02d", h, m)
                                viewModel.updateUserProfile(userId, mapOf("notificationTime" to newTime))
                            }, hour, minute, true).show()
                        }
                    )
                }

                item {
                    AccountSection(onLogout = onLogout)
                }
            }
        }
    }
}

@Composable
fun DailyTargetDialog(initialValue: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var text by remember { mutableStateOf(initialValue.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Daily Target") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { if (it.all { char -> char.isDigit() }) text = it },
                label = { Text("Words per day") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text.toIntOrNull() ?: initialValue) }) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun UserInfoSection(name: String, email: String, level: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            modifier = Modifier.size(80.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Icon(
                Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.padding(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(email, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "Level: $level",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@Composable
fun LearningGoalSection(currentGoal: String, onGoalChange: (String) -> Unit) {
    val goals = listOf("IELTS", "TOEIC", "Communication", "General")
    var expanded by remember { mutableStateOf(false) }

    Column {
        Text("Learning Goal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Box {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth().clickable { expanded = true },
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(currentGoal)
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                goals.forEach { goal ->
                    DropdownMenuItem(
                        text = { Text(goal) },
                        onClick = {
                            onGoalChange(goal)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsSection(
    dailyTarget: Int,
    isDarkMode: Boolean,
    notificationTime: String,
    onDailyTargetClick: () -> Unit,
    onDarkModeToggle: (Boolean) -> Unit,
    onNotificationTimeClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        
        SettingItem(
            icon = Icons.Default.Flag,
            title = "Daily Target",
            subtitle = "$dailyTarget words/day",
            onClick = onDailyTargetClick
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.DarkMode, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(16.dp))
                Text("Dark Mode")
            }
            Switch(checked = isDarkMode, onCheckedChange = onDarkModeToggle)
        }

        SettingItem(
            icon = Icons.Default.Notifications,
            title = "Notification Time",
            subtitle = notificationTime,
            onClick = onNotificationTimeClick
        )
    }
}

@Composable
fun SettingItem(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
        Spacer(modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
    }
}

@Composable
fun AccountSection(onLogout: () -> Unit) {
    Column {
        Text("Account", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Default.Logout, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Logout")
        }
    }
}
