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
fun NoteDetailScreen(
    navController: NavController,
    viewModel: VaultViewModel,
    noteId: Int
) {
    val allNotes by viewModel.allNotes.collectAsState()
    val currentEntry = allNotes.find { it.id == noteId }

    if (currentEntry == null) return

    val context = LocalContext.current

    // FIXED: Using the standard Android ClipboardManager
    val clipboardManager = remember {
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }

    val coroutineScope = rememberCoroutineScope()

    var title by remember { mutableStateOf(currentEntry.title) }
    var content by remember { mutableStateOf("••••••••••••••••••••••••") }
    var contentVisible by remember { mutableStateOf(false) }
    var isDecrypted by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Secure Note", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val textToCopy = if (isDecrypted) content else viewModel.decryptSecret(currentEntry.encryptedContent)

                        val clip = ClipData.newPlainText("Vault Note", textToCopy)
                        clipboardManager.setPrimaryClip(clip)

                        Toast.makeText(context, "Copied! Clipboard clears in 30s.", Toast.LENGTH_SHORT).show()

                        coroutineScope.launch {
                            delay(30000)
                            val emptyClip = ClipData.newPlainText("Cleared", "")
                            clipboardManager.setPrimaryClip(emptyClip)
                        }
                    }) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy Note")
                    }

                    IconButton(
                        onClick = {
                            viewModel.deleteNote(currentEntry)
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
                label = { Text("Note Title") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("Secure Content") },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                visualTransformation = if (contentVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val image = if (contentVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                    IconButton(
                        onClick = {
                            if (!isDecrypted) {
                                content = viewModel.decryptSecret(currentEntry.encryptedContent)
                                isDecrypted = true
                            }
                            contentVisible = !contentVisible
                        }
                    ) {
                        Icon(imageVector = image, contentDescription = "Toggle Visibility")
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val textToSave = if (isDecrypted) content else viewModel.decryptSecret(currentEntry.encryptedContent)
                    val finalEntryToSave = currentEntry.copy(
                        title = title.trim(),
                        encryptedContent = textToSave
                    )

                    viewModel.updateNote(finalEntryToSave)
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