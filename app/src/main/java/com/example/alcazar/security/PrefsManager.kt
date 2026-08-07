package com.example.alcazar.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate

class PrefsManager(context: Context, private val cryptoManager: CryptoManager) {

    private val prefs: SharedPreferences = context.getSharedPreferences("alcazar_settings", Context.MODE_PRIVATE)
    private val settingsAlias = cryptoManager.ALIAS_REAL

    init {
        Log.d("PrefsManager", "PrefsManager initialized")
    }

    // --- Onboarding & Identity State ---
    fun setOnboardingComplete(isComplete: Boolean) {
        Log.d("PrefsManager", "setOnboardingComplete: $isComplete")
        prefs.edit().putBoolean("onboarding_complete", isComplete).apply()
    }

    fun isOnboardingComplete(): Boolean {
        return prefs.getBoolean("onboarding_complete", false)
    }

    fun saveUsername(username: String) {
        Log.d("PrefsManager", "saveUsername: $username")
        prefs.edit().putString("vault_username", username).apply()
    }

    fun getUsername(): String {
        return prefs.getString("vault_username", "Operator") ?: "Operator"
    }

    // --- Master Key Logic ---
    fun saveMasterKey(key: String) {
        Log.d("PrefsManager", "saveMasterKey called")
        try {
            val encryptedKey = cryptoManager.encrypt(key, settingsAlias)
            prefs.edit().putString("master_key", encryptedKey).apply()
            Log.d("PrefsManager", "Master key saved successfully")
        } catch (e: Exception) {
            Log.e("PrefsManager", "Failed to save master key: ${e.message}", e)
            prefs.edit().putString("master_key", "PLAINTEXT:$key").apply()
            Log.d("PrefsManager", "Saved master key as plaintext fallback")
        }
    }

    fun getMasterKey(): String? {
        Log.d("PrefsManager", "getMasterKey called")
        return try {
            val encryptedKey = prefs.getString("master_key", null)
            Log.d("PrefsManager", "Retrieved encrypted key, length: ${encryptedKey?.length ?: 0}")
            if (encryptedKey != null) {
                val decrypted = cryptoManager.decrypt(encryptedKey, settingsAlias)
                Log.d("PrefsManager", "Decrypted key, length: ${decrypted.length}")
                decrypted
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("PrefsManager", "Failed to get master key: ${e.message}", e)
            null
        }
    }

    // --- Duress Key Logic (Write-Once Lock) ---
    fun saveDuressKey(key: String) {
        Log.d("PrefsManager", "saveDuressKey called")
        try {
            if (!prefs.contains("duress_key")) {
                val encryptedKey = cryptoManager.encrypt(key, settingsAlias)
                prefs.edit().putString("duress_key", encryptedKey).apply()
                Log.d("PrefsManager", "Duress key saved successfully")
            } else {
                Log.d("PrefsManager", "Duress key already exists, skipping save")
            }
        } catch (e: Exception) {
            Log.e("PrefsManager", "Failed to save duress key: ${e.message}", e)
            if (!prefs.contains("duress_key")) {
                prefs.edit().putString("duress_key", "PLAINTEXT:$key").apply()
                Log.d("PrefsManager", "Saved duress key as plaintext fallback")
            }
        }
    }

    fun getDuressKey(): String? {
        Log.d("PrefsManager", "getDuressKey called")
        return try {
            val encryptedKey = prefs.getString("duress_key", null)
            Log.d("PrefsManager", "Retrieved encrypted duress key, length: ${encryptedKey?.length ?: 0}")
            if (encryptedKey != null) {
                val decrypted = cryptoManager.decrypt(encryptedKey, settingsAlias)
                Log.d("PrefsManager", "Decrypted duress key, length: ${decrypted.length}")
                decrypted
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("PrefsManager", "Failed to get duress key: ${e.message}", e)
            null
        }
    }

    // ---------- NEW THEME FUNCTIONS ----------
    fun setThemeMode(mode: Int) {
        prefs.edit().putInt("theme_mode", mode).apply()
    }

    fun getThemeMode(): Int {
        return prefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
    }
}