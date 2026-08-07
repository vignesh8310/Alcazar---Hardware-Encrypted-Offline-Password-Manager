package com.example.alcazar

import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.Modifier
import com.example.alcazar.data.database.AlcazarDatabase
import com.example.alcazar.data.repository.VaultRepository
import com.example.alcazar.navigation.Navigation
import com.example.alcazar.security.CryptoManager
import com.example.alcazar.security.PrefsManager
import com.example.alcazar.ui.theme.AlcazarTheme
import com.example.alcazar.viewmodel.VaultViewModel
import com.example.alcazar.viewmodel.VaultViewModelFactory

class MainActivity : ComponentActivity() {

    private val database by lazy { AlcazarDatabase.getDatabase(this) }
    private val cryptoManager by lazy { CryptoManager() }
    private val prefsManager by lazy { PrefsManager(this, cryptoManager) }
    private val repository by lazy { VaultRepository(database.passwordDao(), database.noteDao()) }

    private val viewModel: VaultViewModel by viewModels {
        VaultViewModelFactory(repository, cryptoManager, prefsManager)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Apply saved theme before setContent
        val themeMode = prefsManager.getThemeMode()
        AppCompatDelegate.setDefaultNightMode(themeMode)

        setContent {
            AlcazarTheme(
                // Pass the saved theme mode as a parameter
                darkTheme = when (themeMode) {
                    AppCompatDelegate.MODE_NIGHT_YES -> true
                    AppCompatDelegate.MODE_NIGHT_NO -> false
                    else -> isSystemInDarkTheme() // fallback
                }
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Navigation(
                        viewModel = viewModel,
                        prefsManager = prefsManager,
                        cryptoManager = cryptoManager,
                        repository = repository,
                        modifier = Modifier
                    )
                }
            }
        }
    }
}