package com.example.alcazar.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.example.alcazar.security.CryptoManager
import com.example.alcazar.security.PrefsManager
import com.example.alcazar.ui.screens.*
import com.example.alcazar.viewmodel.VaultViewModel

@Composable
fun Navigation(
    viewModel: VaultViewModel,
    navController: NavHostController = rememberNavController(),
    prefsManager: PrefsManager? = null,
    cryptoManager: CryptoManager? = null,
    repository: Any? = null,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
        modifier = modifier
    ) {
        composable(Screen.Splash.route) {
            SecuritySplashScreen(
                navController = navController,
                prefsManager = prefsManager ?: return@composable
            )
        }

        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                navController = navController,
                prefsManager = prefsManager ?: return@composable
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(
                viewModel = viewModel,
                navController = navController
            )
        }

        // Nested graph for main screens (bottom nav)
        navigation(
            startDestination = Screen.Home.route,
            route = Screen.Main.route
        ) {
            composable(Screen.Home.route) {
                MainScreen(
                    viewModel = viewModel,
                    navController = navController
                ) { innerPadding ->
                    HomeScreen(
                        viewModel = viewModel,
                        navController = navController,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
            composable(Screen.Passwords.route) {
                MainScreen(
                    viewModel = viewModel,
                    navController = navController
                ) { innerPadding ->
                    PasswordsScreen(
                        navController = navController,
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
            composable(Screen.Notes.route) {
                MainScreen(
                    viewModel = viewModel,
                    navController = navController
                ) { innerPadding ->
                    NotesScreen(
                        navController = navController,
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
            composable(Screen.Settings.route) {
                MainScreen(
                    viewModel = viewModel,
                    navController = navController
                ) { innerPadding ->
                    SettingsScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }

        // Full‑screen overlays (outside bottom nav)
        composable(Screen.AddPassword.route) {
            AddPasswordScreen(viewModel = viewModel, navController = navController)
        }

        composable(Screen.AddNote.route) {
            AddNoteScreen(viewModel = viewModel, navController = navController)
        }

        composable(
            route = "password_detail/{passwordId}",
            arguments = listOf(navArgument("passwordId") { defaultValue = 0 })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getInt("passwordId") ?: 0
            PasswordDetailScreen(
                navController = navController,
                viewModel = viewModel,
                passwordId = id
            )
        }

        composable(
            route = "note_detail/{noteId}",
            arguments = listOf(navArgument("noteId") { defaultValue = 0 })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getInt("noteId") ?: 0
            NoteDetailScreen(
                navController = navController,
                viewModel = viewModel,
                noteId = id
            )
        }
    }
}