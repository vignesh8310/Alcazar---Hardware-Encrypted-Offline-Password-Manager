package com.example.alcazar.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Onboarding : Screen("onboarding_flow")
    object Login : Screen("login")
    object Main : Screen("main")          // <-- new root for bottom nav
    object Home : Screen("home")
    object Passwords : Screen("passwords")
    object Notes : Screen("notes")
    object Settings : Screen("settings")
    object AddPassword : Screen("add_password")
    object AddNote : Screen("add_note")
}