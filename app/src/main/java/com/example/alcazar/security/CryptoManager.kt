package com.example.alcazar.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class CryptoManager {

    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply {
        load(null)
    }

    private val TRANSFORMATION = "AES/GCM/NoPadding"

    val ALIAS_REAL = "alcazar_real_key"
    val ALIAS_DURESS = "alcazar_duress_key"

    init {
        Log.d("CryptoManager", "Initializing CryptoManager")
        try {
            // Try to create keys if they don't exist
            if (!keyStore.containsAlias(ALIAS_REAL)) {
                Log.d("CryptoManager", "Creating REAL key")
                createKey(ALIAS_REAL)
            }
            if (!keyStore.containsAlias(ALIAS_DURESS)) {
                Log.d("CryptoManager", "Creating DURESS key")
                createKey(ALIAS_DURESS)
            }
            Log.d("CryptoManager", "CryptoManager initialized successfully")
        } catch (e: Exception) {
            Log.e("CryptoManager", "Failed to create keys: ${e.message}", e)
        }
    }

    private fun createKey(alias: String) {
        try {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")

            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()

            keyGenerator.init(keyGenParameterSpec)
            keyGenerator.generateKey()
            Log.d("CryptoManager", "Key created successfully: $alias")
        } catch (e: Exception) {
            Log.e("CryptoManager", "Failed to create key $alias: ${e.message}", e)
            throw e
        }
    }

    private fun getSecretKey(alias: String): SecretKey? {
        return try {
            if (keyStore.containsAlias(alias)) {
                keyStore.getKey(alias, null) as SecretKey
            } else {
                Log.w("CryptoManager", "Key not found: $alias")
                null
            }
        } catch (e: Exception) {
            Log.e("CryptoManager", "Failed to get key $alias: ${e.message}", e)
            null
        }
    }

    fun encrypt(unencryptedText: String, alias: String): String {
        if (unencryptedText.isBlank()) return ""

        Log.d("CryptoManager", "Encrypting with alias: $alias, text length: ${unencryptedText.length}")

        return try {
            val secretKey = getSecretKey(alias)
            if (secretKey == null) {
                Log.w("CryptoManager", "No key found for $alias, returning plaintext")
                return unencryptedText
            }

            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)

            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(unencryptedText.toByteArray(Charsets.UTF_8))
            val combined = iv + encryptedBytes

            val result = Base64.encodeToString(combined, Base64.DEFAULT)
            Log.d("CryptoManager", "Encryption successful, result length: ${result.length}")
            result
        } catch (e: Exception) {
            Log.e("CryptoManager", "Encryption failed: ${e.message}", e)
            // Return plaintext with a marker so we know it wasn't encrypted
            "PLAINTEXT:$unencryptedText"
        }
    }

    fun decrypt(encryptedText: String, alias: String): String {
        if (encryptedText.isBlank()) return ""

        // Check if it's a plaintext fallback
        if (encryptedText.startsWith("PLAINTEXT:")) {
            Log.d("CryptoManager", "Decrypting plaintext fallback")
            return encryptedText.substringAfter("PLAINTEXT:")
        }

        Log.d("CryptoManager", "Decrypting with alias: $alias, text length: ${encryptedText.length}")

        return try {
            val secretKey = getSecretKey(alias)
            if (secretKey == null) {
                Log.w("CryptoManager", "No key found for $alias, returning ciphertext")
                return encryptedText
            }

            val combined = Base64.decode(encryptedText, Base64.DEFAULT)

            if (combined.size < 12) {
                Log.w("CryptoManager", "Invalid encrypted data, returning as is")
                return encryptedText
            }

            val iv = combined.copyOfRange(0, 12)
            val encryptedBytes = combined.copyOfRange(12, combined.size)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

            val decryptedBytes = cipher.doFinal(encryptedBytes)
            val result = String(decryptedBytes, Charsets.UTF_8)
            Log.d("CryptoManager", "Decryption successful, result length: ${result.length}")
            result
        } catch (e: Exception) {
            Log.e("CryptoManager", "Decryption failed: ${e.message}", e)
            encryptedText
        }
    }
}