package com.example.alcazar.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.alcazar.data.model.PasswordEntry
import com.example.alcazar.viewmodel.VaultViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPasswordScreen(navController: NavController, viewModel: VaultViewModel) {
    var title by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // Category States
    val dynamicCategories by viewModel.dynamicCategories.collectAsState()
    var category by remember { mutableStateOf("All") }
    var expanded by remember { mutableStateOf(false) }

    // Dialog States
    var showNewCategoryDialog by remember { mutableStateOf(false) }
    var newCategoryInput by remember { mutableStateOf("") }

    fun generateRandomPassword(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()_+-="
        return (1..16).map { chars.random() }.joinToString("")
    }

    if (showNewCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showNewCategoryDialog = false },
            title = { Text("New Category") },
            text = {
                OutlinedTextField(
                    value = newCategoryInput,
                    onValueChange = { newCategoryInput = it },
                    label = { Text("Category Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newCategoryInput.isNotBlank()) {
                        category = newCategoryInput.trim()
                    }
                    showNewCategoryDialog = false
                    newCategoryInput = ""
                }) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showNewCategoryDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Password", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                trailingIcon = {
                    IconButton(onClick = { password = generateRandomPassword() }) {
                        Icon(imageVector = Icons.Default.AutoFixHigh, contentDescription = "Generate", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Dropdown Menu Logic
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = category,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    dynamicCategories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat) },
                            onClick = {
                                category = cat
                                expanded = false
                            }
                        )
                    }
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("➕ Add New Category...", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
                        onClick = {
                            expanded = false
                            showNewCategoryDialog = true
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    val finalCategory = if (category.isBlank()) "All" else category.trim()
                    val newEntry = PasswordEntry(
                        title = title.trim(),
                        username = username.trim(),
                        encryptedPassword = password,
                        category = finalCategory
                    )
                    viewModel.addPassword(newEntry)
                    navController.popBackStack()
                },
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp).height(50.dp),
                enabled = title.isNotBlank() && password.isNotBlank()
            ) {
                Text("Save Password", fontSize = MaterialTheme.typography.titleMedium.fontSize)
            }
        }
    }
}