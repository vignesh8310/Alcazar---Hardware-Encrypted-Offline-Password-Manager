package com.example.alcazar.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.example.alcazar.ui.components.RecentEntryCard
import com.example.alcazar.ui.components.SearchBar
import com.example.alcazar.viewmodel.VaultViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PasswordsScreen(
    navController: NavController,
    viewModel: VaultViewModel,
    modifier: Modifier = Modifier
) {
    val realPasswords by viewModel.allPasswords.collectAsState()
    val dynamicCategories by viewModel.dynamicCategories.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    var selectedIds by remember { mutableStateOf(setOf<Int>()) }
    val isSelectionMode = selectedIds.isNotEmpty()

    val filteredPasswords = realPasswords.filter { passwordEntry ->
        val matchesSearch = passwordEntry.title.contains(searchQuery, ignoreCase = true) ||
                passwordEntry.username.contains(searchQuery, ignoreCase = true)
        val matchesCategory = selectedCategory == "All" || passwordEntry.category == selectedCategory
        matchesSearch && matchesCategory
    }

    // The FAB is placed inside a Scaffold that wraps this screen.
    // Since we are inside MainScreen's Scaffold, we need to use a new Scaffold here.
    // But to avoid nested Scaffolds, we'll use a Box with a FAB.
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
                    text = "Passwords",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                if (isSelectionMode) {
                    Button(
                        onClick = {
                            selectedIds.forEach { id ->
                                val passwordToDelete = realPasswords.find { it.id == id }
                                if (passwordToDelete != null) {
                                    viewModel.deletePassword(passwordToDelete)
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

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(dynamicCategories.size) { index ->
                    val category = dynamicCategories[index]
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = { Text(category) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (filteredPasswords.isEmpty()) {
                Text(
                    text = "No passwords found.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 32.dp)
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredPasswords.size) { index ->
                        val entry = filteredPasswords[index]
                        val isSelected = selectedIds.contains(entry.id)

                        Box(
                            modifier = Modifier.combinedClickable(
                                onClick = {
                                    if (isSelectionMode) {
                                        selectedIds = if (isSelected) {
                                            selectedIds - entry.id
                                        } else {
                                            selectedIds + entry.id
                                        }
                                    } else {
                                        navController.navigate("password_detail/${entry.id}")
                                    }
                                },
                                onLongClick = {
                                    selectedIds = selectedIds + entry.id
                                }
                            )
                        ) {
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                RecentEntryCard(
                                    title = entry.title,
                                    username = entry.username
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
                onClick = { navController.navigate(Screen.AddPassword.route) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Password")
            }
        }
    }
}