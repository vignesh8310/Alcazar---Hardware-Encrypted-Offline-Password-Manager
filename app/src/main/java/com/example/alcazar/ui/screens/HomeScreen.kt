package com.example.alcazar.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.alcazar.navigation.Screen
import com.example.alcazar.ui.components.HomeHeader
import com.example.alcazar.ui.components.StatsCard
import com.example.alcazar.viewmodel.VaultViewModel

@Composable
fun HomeScreen(
    viewModel: VaultViewModel,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    // 1. Observe the list of passwords from VaultViewModel
    val passwords by viewModel.allPasswords.collectAsState()

    // 2. Observe the list of notes from VaultViewModel
    val notes by viewModel.allNotes.collectAsState()

    // 3. Dynamically collect the saved user name from VaultViewModel state
    val userName by viewModel.userName.collectAsState(initial = "User")

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // 4. Pass the dynamic userName state variable instead of "Vignesh"
        HomeHeader(userName = userName)

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatsCard(
                title = "Passwords",
                count = passwords.size.toString(),
                modifier = Modifier.weight(1f),
                onClick = { navController.navigate(Screen.Passwords.route) }
            )
            StatsCard(
                title = "Secure Notes",
                count = notes.size.toString(),
                modifier = Modifier.weight(1f),
                onClick = { navController.navigate(Screen.Notes.route) }
            )
        }
    }
}