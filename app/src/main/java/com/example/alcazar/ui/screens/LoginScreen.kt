package com.example.alcazar.ui.screens

import android.util.Log
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.alcazar.navigation.Screen
import com.example.alcazar.viewmodel.VaultViewModel
import java.util.concurrent.Executor

@Composable
fun LoginScreen(
    viewModel: VaultViewModel,
    navController: NavController
) {
    val context = LocalContext.current
    var passcode by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isBiometricHardwarePresent by remember { mutableStateOf(false) }

    // Check if device has fingerprint hardware (ignore enrollment state for Oppo)
    LaunchedEffect(Unit) {
        try {
            val hasHardware = context.packageManager.hasSystemFeature(
                android.content.pm.PackageManager.FEATURE_FINGERPRINT
            )
            isBiometricHardwarePresent = hasHardware
            Log.d("LoginScreen", "Fingerprint hardware present: $hasHardware")
        } catch (e: Exception) {
            Log.e("LoginScreen", "Error checking hardware: ${e.message}")
            isBiometricHardwarePresent = false
        }
    }

    val fragmentActivity = remember { context.findFragmentActivity() }
    val executor: Executor = remember { ContextCompat.getMainExecutor(context) }

    // Always create BiometricPrompt if hardware is present, regardless of enrollment status
    val biometricPrompt = remember(fragmentActivity, isBiometricHardwarePresent) {
        if (fragmentActivity != null && isBiometricHardwarePresent) {
            try {
                BiometricPrompt(
                    fragmentActivity,
                    executor,
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            viewModel.setVaultSession(false, "BIOMETRIC")
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Login.route) { inclusive = true }
                            }
                        }

                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                            // If error says no biometrics enrolled, fallback gracefully
                            when (errorCode) {
                                BiometricPrompt.ERROR_NO_BIOMETRICS,
                                BiometricPrompt.ERROR_HW_NOT_PRESENT,
                                BiometricPrompt.ERROR_HW_UNAVAILABLE -> {
                                    Toast.makeText(context, "Fingerprint not set up. Use passcode.", Toast.LENGTH_SHORT).show()
                                }
                                else -> {
                                    Toast.makeText(context, "Biometric error: $errString", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }

                        override fun onAuthenticationFailed() {
                            Toast.makeText(context, "Authentication failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e("LoginScreen", "Failed to create BiometricPrompt: ${e.message}")
                null
            }
        } else null
    }

    val promptInfo = remember {
        BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Alcázar Vault")
            .setSubtitle("Authenticate using your fingerprint")
            .setAllowedAuthenticators(BIOMETRIC_STRONG)
            .setNegativeButtonText("Use Master Key")
            .build()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Vault Lock",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Alcázar Vault",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = if (isBiometricHardwarePresent) "Enter your Master Key or use fingerprint" else "Enter your Master Key to unlock",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = passcode,
                onValueChange = { passcode = it },
                label = { Text("Master Key / Passcode") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                        Icon(
                            imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (isPasswordVisible) "Hide password" else "Show password"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (passcode.isBlank()) {
                        Toast.makeText(context, "Please enter your passcode", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    when (viewModel.verifyPasscode(passcode)) {
                        "REAL" -> {
                            viewModel.setVaultSession(false, passcode)
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Login.route) { inclusive = true }
                            }
                        }
                        "DURESS" -> {
                            viewModel.setVaultSession(true, passcode)
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Login.route) { inclusive = true }
                            }
                        }
                        else -> Toast.makeText(context, "Invalid Master Key", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Unlock Vault", style = MaterialTheme.typography.titleMedium)
            }

            // Show fingerprint button if hardware is present (even if no enrollment)
            if (isBiometricHardwarePresent && biometricPrompt != null) {
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = {
                        try {
                            biometricPrompt.authenticate(promptInfo)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Biometric error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = "Fingerprint Icon",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Use Fingerprint", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}