package com.example.alcazar.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.alcazar.navigation.Screen
import com.example.alcazar.ui.components.SearchBar
import com.example.alcazar.viewmodel.VaultViewModel

@Composable
fun SecureNoteCard(title: String, snippet: String, date: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = snippet, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NotesScreen(
    navController: NavController,
    viewModel: VaultViewModel,
    modifier: Modifier = Modifier
) {
    val realNotes by viewModel.allNotes.collectAsState()

    var searchQuery by remember { mutableStateOf("") }

    var selectedIds by remember { mutableStateOf(setOf<Int>()) }
    val isSelectionMode = selectedIds.isNotEmpty()

    val filteredNotes = realNotes.filter { noteEntry ->
        noteEntry.title.contains(searchQuery, ignoreCase = true)
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Secure Notes",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                if (isSelectionMode) {
                    Button(
                        onClick = {
                            selectedIds.forEach { id ->
                                val noteToDelete = realNotes.find { it.id == id }
                                if (noteToDelete != null) {
                                    viewModel.deleteNote(noteToDelete)
                                }
                            }
                            selectedIds = emptySet()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete ${selectedIds.size}")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (filteredNotes.isEmpty()) {
                Text(
                    text = "No secure notes found.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 32.dp)
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredNotes.size) { index ->
                        val note = filteredNotes[index]
                        val isSelected = selectedIds.contains(note.id)

                        Box(
                            modifier = Modifier.combinedClickable(
                                onClick = {
                                    if (isSelectionMode) {
                                        selectedIds = if (isSelected) {
                                            selectedIds - note.id
                                        } else {
                                            selectedIds + note.id
                                        }
                                    } else {
                                        navController.navigate("note_detail/${note.id}")
                                    }
                                },
                                onLongClick = {
                                    selectedIds = selectedIds + note.id
                                }
                            )
                        ) {
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                SecureNoteCard(
                                    title = note.title,
                                    snippet = "••••••••••••",
                                    date = "Saved Securely"
                                )
                            }
                        }
                    }
                }
            }
        }

        // Floating Action Button
        if (!isSelectionMode) {
            FloatingActionButton(
                onClick = { navController.navigate(Screen.AddNote.route) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Note")
            }
        }
    }
}