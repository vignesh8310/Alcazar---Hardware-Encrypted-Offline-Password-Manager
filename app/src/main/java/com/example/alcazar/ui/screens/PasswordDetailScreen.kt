package com.example.alcazar.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.alcazar.viewmodel.VaultViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordDetailScreen(
    navController: NavController,
    viewModel: VaultViewModel,
    passwordId: Int
) {
    val allPasswords by viewModel.allPasswords.collectAsState()
    val currentEntry = allPasswords.find { it.id == passwordId }

    if (currentEntry == null) return

    val context = LocalContext.current

    // FIXED: Using the standard Android ClipboardManager to avoid deprecation warnings
    val clipboardManager = remember {
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }

    val coroutineScope = rememberCoroutineScope()

    var title by remember { mutableStateOf(currentEntry.title) }
    var username by remember { mutableStateOf(currentEntry.username) }
    var password by remember { mutableStateOf("••••••••••••••••") }
    var category by remember { mutableStateOf(currentEntry.category) }

    var passwordVisible by remember { mutableStateOf(false) }
    var isDecrypted by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Password", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.deletePassword(currentEntry)
                            navController.popBackStack()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Website or App Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username / Email") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    Row {
                        IconButton(onClick = {
                            val textToCopy = if (isDecrypted) password else viewModel.decryptSecret(currentEntry.encryptedPassword)

                            // Native Clipboard implementation
                            val clip = ClipData.newPlainText("Vault Password", textToCopy)
                            clipboardManager.setPrimaryClip(clip)

                            Toast.makeText(context, "Copied! Clipboard clears in 30s.", Toast.LENGTH_SHORT).show()

                            coroutineScope.launch {
                                delay(30000)
                                // Wipes the clipboard after 30 seconds
                                val emptyClip = ClipData.newPlainText("Cleared", "")
                                clipboardManager.setPrimaryClip(emptyClip)
                            }
                        }) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy Password")
                        }

                        IconButton(onClick = {
                            if (!isDecrypted) {
                                password = viewModel.decryptSecret(currentEntry.encryptedPassword)
                                isDecrypted = true
                            }
                            passwordVisible = !passwordVisible
                        }) {
                            val icon = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                            Icon(imageVector = icon, contentDescription = "Toggle Visibility")
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                label = { Text("Category") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    val textToSave = if (isDecrypted) password else viewModel.decryptSecret(currentEntry.encryptedPassword)
                    val finalEntryToSave = currentEntry.copy(
                        title = title.trim(),
                        username = username.trim(),
                        category = if (category.isBlank()) "All" else category.trim(),
                        encryptedPassword = textToSave
                    )

                    viewModel.updatePassword(finalEntryToSave)
                    navController.popBackStack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
                    .height(50.dp)
            ) {
                Text("Save Updates", fontSize = MaterialTheme.typography.titleMedium.fontSize)
            }
        }
    }
}