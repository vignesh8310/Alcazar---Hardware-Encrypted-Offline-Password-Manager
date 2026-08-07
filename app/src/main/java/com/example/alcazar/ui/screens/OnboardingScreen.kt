package com.example.alcazar.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.alcazar.navigation.Screen
import com.example.alcazar.security.PrefsManager
import kotlinx.coroutines.launch

// ---- Top‑level page composables ----

@Composable
fun IntroPage() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(imageVector = Icons.Default.Security, contentDescription = "Shield", modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(24.dp))
        Text("Alcázar", style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Black)
        Spacer(modifier = Modifier.height(32.dp))

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            FeatureRow(Icons.Default.VpnKey, "Military-Grade AES-256 Encryption")
            FeatureRow(Icons.Default.WifiOff, "Offline-First Zero-Trust Architecture")
            FeatureRow(Icons.Default.Memory, "Hardware-Bound Keystore Protection")
            FeatureRow(Icons.Default.Warning, "Plausible Deniability (Duress Vault)")
            FeatureRow(Icons.Default.Sync, "Cross-Device Envelope Backups")
        }
    }
}

@Composable
fun FeatureRow(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = text, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun UsernamePage(username: String, onValueChange: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Identify", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Text("What should the vault call you?", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(48.dp))
        OutlinedTextField(
            value = username,
            onValueChange = onValueChange,
            placeholder = { Text("Enter your alias", style = MaterialTheme.typography.titleLarge) },
            textStyle = MaterialTheme.typography.headlineMedium,
            singleLine = true,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun MasterKeyPage(key: String, onValueChange: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Master Key", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Create a highly secure passcode. If you forget this, your data is mathematically unrecoverable.", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(48.dp))
        OutlinedTextField(
            value = key,
            onValueChange = onValueChange,
            placeholder = { Text("Enter Primary Passcode") },
            textStyle = MaterialTheme.typography.headlineMedium,
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun DuressKeyPage(key: String, onValueChange: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Duress Key", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
        Spacer(modifier = Modifier.height(16.dp))
        Text("If forced to unlock your device, enter this passcode instead. It will open an empty decoy vault. Choose a realistic fake password.", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(48.dp))
        OutlinedTextField(
            value = key,
            onValueChange = onValueChange,
            placeholder = { Text("Enter Decoy Passcode") },
            textStyle = MaterialTheme.typography.headlineMedium,
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth()
        )
        AnimatedVisibility(visible = key.isNotEmpty() && key == "Admin123") {
            Text("Cannot be 'Admin123'", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
fun FinalizePage(termsAccepted: Boolean, onTermsChanged: (Boolean) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(imageVector = Icons.Default.GppGood, contentDescription = "Secure", modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(24.dp))
        Text("Ready.", style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Black)
        Spacer(modifier = Modifier.height(24.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = termsAccepted,
                    onCheckedChange = onTermsChanged,
                    colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.error)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "I accept the Terms and Conditions. I understand that Alcázar is a zero-knowledge architecture. If I lose my Master Key and Recovery Key, my data is mathematically unrecoverable. The developers hold zero liability for lost data.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

// ---- OnboardingScreen (the main screen) ----
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(navController: NavController, prefsManager: PrefsManager) {
    val pagerState = rememberPagerState(pageCount = { 5 })
    val coroutineScope = rememberCoroutineScope()

    var username by remember { mutableStateOf("") }
    var masterKey by remember { mutableStateOf("") }
    var duressKey by remember { mutableStateOf("") }
    var termsAccepted by remember { mutableStateOf(false) }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = false,
                modifier = Modifier.weight(1f)
            ) { page ->
                when (page) {
                    0 -> IntroPage()
                    1 -> UsernamePage(username) { username = it }
                    2 -> MasterKeyPage(masterKey) { masterKey = it }
                    3 -> DuressKeyPage(duressKey) { duressKey = it }
                    4 -> FinalizePage(termsAccepted) { termsAccepted = it }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(bottom = 32.dp)
                ) {
                    repeat(5) { iteration ->
                        val color = if (pagerState.currentPage == iteration) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        Box(
                            modifier = Modifier
                                .padding(4.dp)
                                .clip(CircleShape)
                                .background(color)
                                .size(if (pagerState.currentPage == iteration) 12.dp else 8.dp)
                        )
                    }
                }

                val isButtonEnabled = when (pagerState.currentPage) {
                    1 -> username.isNotBlank()
                    2 -> masterKey.length >= 4
                    3 -> duressKey.length >= 4 && duressKey != masterKey
                    4 -> termsAccepted
                    else -> true
                }

                Button(
                    onClick = {
                        if (pagerState.currentPage < 4) {
                            coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        } else {
                            prefsManager.saveUsername(username.trim())
                            prefsManager.saveMasterKey(masterKey.trim())
                            prefsManager.saveDuressKey(duressKey.trim())
                            prefsManager.setOnboardingComplete(true)
                            navController.navigate(Screen.Login.route) {
                                popUpTo(Screen.Onboarding.route) { inclusive = true }
                            }
                        }
                    },
                    enabled = isButtonEnabled,
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier.fillMaxWidth().height(64.dp)
                ) {
                    if (pagerState.currentPage == 4) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = "Finish")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Initialize Vault", fontSize = MaterialTheme.typography.titleLarge.fontSize)
                    } else {
                        Text("Continue", fontSize = MaterialTheme.typography.titleLarge.fontSize)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(imageVector = Icons.Default.ArrowForward, contentDescription = "Next")
                    }
                }
            }
        }
    }
}