package com.example.alcazar.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate   // ADDED
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.alcazar.viewmodel.VaultViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: VaultViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }

    var newMasterKey by remember { mutableStateOf("") }
    var oldMasterKeyInput by remember { mutableStateOf("") }
    var showAuthDialog by remember { mutableStateOf(false) }
    var showFaqDialog by remember { mutableStateOf(false) }

    // --- Migration UI States ---
    var showExportDialog by rememberSaveable { mutableStateOf(false) }
    var showImportDialog by rememberSaveable { mutableStateOf(false) }
    var migrationPassword by rememberSaveable { mutableStateOf("") }
    var generatedRecoveryKey by rememberSaveable { mutableStateOf("") }
    var isUsingRecoveryKey by rememberSaveable { mutableStateOf(false) }
    var pendingImportContent by remember { mutableStateOf<String?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val pass = migrationPassword
        val recKey = generatedRecoveryKey
        coroutineScope.launch {
            var isSuccess = false
            var errDetail = ""
            try {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        isSuccess = viewModel.exportVaultForMigration(pass, recKey, stream)
                    }
                }
            } catch (t: Throwable) {
                errDetail = t.localizedMessage ?: "File Stream Error"
            } finally {
                migrationPassword = ""
                generatedRecoveryKey = ""
            }
            if (isSuccess) {
                Toast.makeText(context, "Backup Exported Successfully!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "Export Failed: $errDetail", Toast.LENGTH_LONG).show()
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        coroutineScope.launch {
            try {
                val fileData = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        stream.bufferedReader().use { it.readText() }
                    }
                }
                if (!fileData.isNullOrBlank()) {
                    pendingImportContent = fileData
                    showImportDialog = true
                } else {
                    Toast.makeText(context, "Selected file is empty.", Toast.LENGTH_SHORT).show()
                }
            } catch (t: Throwable) {
                Toast.makeText(context, "Cannot read file: ${t.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // --- Biometrics Initialization ---
    val fragmentActivity = remember { context.findFragmentActivity() }
    val executor = remember { ContextCompat.getMainExecutor(context) }

    val biometricPrompt = remember(fragmentActivity) {
        fragmentActivity?.let { activity ->
            BiometricPrompt(
                activity,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        viewModel.updateMasterKey(newMasterKey.trim())
                        newMasterKey = ""
                        showAuthDialog = false
                        Toast.makeText(context, "Master Key Updated via Biometrics", Toast.LENGTH_SHORT).show()
                    }
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        Toast.makeText(context, "Biometric failed, use old password.", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }

    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Verify Identity")
        .setSubtitle("Authenticate to change Master Key")
        .setAllowedAuthenticators(BIOMETRIC_STRONG)
        .setNegativeButtonText("Cancel")
        .build()

    // --- Master Key Change Dialog ---
    if (showAuthDialog) {
        AlertDialog(
            onDismissRequest = { showAuthDialog = false },
            title = { Text("Authentication Required") },
            text = {
                Column {
                    Text("Enter your current Master Key or use your fingerprint to authorize this change.")
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = oldMasterKeyInput,
                        onValueChange = { oldMasterKeyInput = it },
                        label = { Text("Current Master Key") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (viewModel.verifyPasscode(oldMasterKeyInput) == "REAL") {
                        viewModel.updateMasterKey(newMasterKey.trim())
                        newMasterKey = ""
                        oldMasterKeyInput = ""
                        showAuthDialog = false
                        Toast.makeText(context, "Master Key Updated", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Incorrect Password", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("Verify") }
            },
            dismissButton = {
                TextButton(onClick = {
                    val prompt = biometricPrompt
                    if (prompt != null) {
                        prompt.authenticate(promptInfo)
                    } else {
                        Toast.makeText(context, "Biometrics not available", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("Use Fingerprint") }
            }
        )
    }

    // --- Export Config Dialog ---
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Create Backup Envelope") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text("1. Create a password for this backup file:", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = migrationPassword,
                        onValueChange = { migrationPassword = it },
                        label = { Text("Backup Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    Text("2. Save your Emergency Recovery Key:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("If you forget your backup password, you can use this key to restore your data. Copy this now; it will never be shown again.", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = generatedRecoveryKey,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center
                            )
                            IconButton(onClick = {
                                clipboardManager.setPrimaryClip(ClipData.newPlainText("Recovery Key", generatedRecoveryKey))
                                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (migrationPassword.length >= 4) {
                        showExportDialog = false
                        try {
                            exportLauncher.launch("alcazar_backup.json")
                        } catch (e: Exception) {
                            Toast.makeText(context, "File Manager error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(context, "Password must be at least 4 chars", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("Encrypt & Save") }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) { Text("Cancel") }
            }
        )
    }

    // --- Import Password Prompt Dialog ---
    if (showImportDialog && pendingImportContent != null) {
        AlertDialog(
            onDismissRequest = {
                showImportDialog = false
                pendingImportContent = null
            },
            title = { Text("Unlock Backup Envelope") },
            text = {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = isUsingRecoveryKey,
                            onCheckedChange = { isUsingRecoveryKey = it }
                        )
                        Text("Use Recovery Key instead of Password", style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = migrationPassword,
                        onValueChange = { migrationPassword = it },
                        label = { Text(if (isUsingRecoveryKey) "Enter 32-Char Recovery Key" else "Enter Backup Password") },
                        visualTransformation = if (isUsingRecoveryKey) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val pass = migrationPassword.trim()
                    val useRecovery = isUsingRecoveryKey
                    val rawData = pendingImportContent!!

                    showImportDialog = false
                    pendingImportContent = null
                    migrationPassword = ""
                    isUsingRecoveryKey = false

                    coroutineScope.launch {
                        var isSuccess = false
                        var errDetail = ""
                        try {
                            withContext(Dispatchers.IO) {
                                ByteArrayInputStream(rawData.toByteArray(Charsets.UTF_8)).use { stream ->
                                    isSuccess = viewModel.importVaultFromMigration(pass, useRecovery, stream)
                                }
                            }
                        } catch (t: Throwable) {
                            errDetail = t.localizedMessage ?: "Import Failed"
                        }
                        if (isSuccess) {
                            Toast.makeText(context, "Vault Successfully Restored!", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "Decryption Failed: $errDetail", Toast.LENGTH_LONG).show()
                        }
                    }
                }) { Text("Decrypt & Import") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showImportDialog = false
                    pendingImportContent = null
                }) { Text("Cancel") }
            }
        )
    }

    // --- Info Dialog ---
    if (showFaqDialog) {
        AlertDialog(
            onDismissRequest = { showFaqDialog = false },
            title = { Text("Help & Features") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(fontWeight = FontWeight.Bold, text = "AES Hardware Encryption:")
                    Text("Your passwords never leave this device. They are encrypted using the physical security chip inside your phone.")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(fontWeight = FontWeight.Bold, text = "Duress Decoy Vault:")
                    Text("Entering your Duress Key at login routes you to a fake database to protect your real data from physical threats.")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(fontWeight = FontWeight.Bold, text = "Envelope Encrypted Backups:")
                    Text("Exporting extracts your database, encrypts it with a Vault Master Key (VMK), and locks the VMK behind a password AND a recovery key so you can safely move data between devices.")
                }
            },
            confirmButton = { TextButton(onClick = { showFaqDialog = false }) { Text("Close") } }
        )
    }

    // --- Main Settings UI ---
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Vault Settings", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { showFaqDialog = true }) {
                        Icon(imageVector = Icons.Default.HelpOutline, contentDescription = "Help")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // --- Card 1: Update Master Key ---
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Security, contentDescription = "Security", tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Update Master Key", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newMasterKey,
                        onValueChange = { newMasterKey = it },
                        label = { Text("New Master Key") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { if (newMasterKey.isNotBlank()) showAuthDialog = true },
                        modifier = Modifier.align(Alignment.End)
                    ) { Text("Save Master Key") }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- NEW: Dark Mode Toggle ---
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Dark Mode",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Switch(
                        checked = when (viewModel.getThemeMode()) {   // CHANGED: use viewModel
                            AppCompatDelegate.MODE_NIGHT_YES -> true
                            else -> false
                        },
                        onCheckedChange = { isChecked ->
                            val newMode = if (isChecked) {
                                AppCompatDelegate.MODE_NIGHT_YES
                            } else {
                                AppCompatDelegate.MODE_NIGHT_NO
                            }
                            viewModel.setThemeMode(newMode)          // CHANGED: use viewModel
                            AppCompatDelegate.setDefaultNightMode(newMode)
                            (context as? ComponentActivity)?.recreate()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- Card 3: Duress Key (locked) ---
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Warning, contentDescription = "Warning", tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Duress Key (LOCKED)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("For security purposes, the Duress Key is locked permanently after onboarding.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- Card 4: Backup & Restore ---
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Encrypted Backup & Restore", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Export your vault in a highly secure envelope format to move your data to a new device.", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        OutlinedButton(
                            onClick = {
                                try {
                                    importLauncher.launch("*/*")
                                } catch (e: Exception) {
                                    Toast.makeText(context, "File Manager error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.Download, contentDescription = "Import", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Import")
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Button(
                            onClick = {
                                try {
                                    generatedRecoveryKey = viewModel.generateNewRecoveryKey()
                                    showExportDialog = true
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error generating key: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.Upload, contentDescription = "Export", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Export")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}