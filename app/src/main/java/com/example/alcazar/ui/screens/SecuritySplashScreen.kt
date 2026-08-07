package com.example.alcazar.ui.screens

import android.view.WindowManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.alcazar.R
import com.example.alcazar.navigation.Screen
import com.example.alcazar.security.PrefsManager
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun SecuritySplashScreen(navController: NavController, prefsManager: PrefsManager) {
    val context = LocalContext.current
    var isCompromised by remember { mutableStateOf(false) }

    fun isDeviceRooted(): Boolean {
        val buildTags = android.os.Build.TAGS
        if (buildTags != null && buildTags.contains("test-keys")) return true
        val paths = arrayOf(
            "/system/app/Superuser.apk", "/sbin/su", "/system/bin/su", "/system/xbin/su",
            "/data/local/xbin/su", "/data/local/bin/su", "/system/sd/xbin/su",
            "/system/bin/failsafe/su", "/data/local/su", "/su/bin/su"
        )
        for (path in paths) {
            if (File(path).exists()) return true
        }
        return false
    }

    LaunchedEffect(Unit) {
        // Block screenshots & screen recording
        (context as? android.app.Activity)?.window?.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        delay(1200) // Brief delay to display splash graphic while scanning environment

        if (isDeviceRooted()) {
            isCompromised = true
        } else {
            val destination = if (prefsManager.isOnboardingComplete()) {
                Screen.Login.route
            } else {
                "onboarding_flow"
            }

            navController.navigate(destination) {
                popUpTo("splash") { inclusive = true }
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Full-screen 9:16 Splash Graphic
        Image(
            painter = painterResource(id = R.drawable.splash_bg), // Ensure filename matches res/drawable/splash_bg
            contentDescription = "Alcázar Splash Screen",
            contentScale = ContentScale.Crop, // Scaled edge-to-edge
            modifier = Modifier.fillMaxSize()
        )

        // Overlay warning if a rooted environment is detected
        if (isCompromised) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier
                    .padding(24.dp)
                    .align(Alignment.Center)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "SECURITY ALERT",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Root environment detected. Access denied.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}