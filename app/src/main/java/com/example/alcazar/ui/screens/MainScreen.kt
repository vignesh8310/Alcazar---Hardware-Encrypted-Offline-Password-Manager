package com.example.alcazar.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.example.alcazar.ui.components.BottomNavigationBar
import com.example.alcazar.viewmodel.VaultViewModel

@Composable
fun MainScreen(
    viewModel: VaultViewModel,
    navController: NavController,
    content: @Composable (PaddingValues) -> Unit
) {
    val context = LocalContext.current

    val errorMessage by viewModel.errorMessage.collectAsState()
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                navController = navController,
                modifier = Modifier
            )
        }
    ) { innerPadding ->
        content(innerPadding)
    }
}