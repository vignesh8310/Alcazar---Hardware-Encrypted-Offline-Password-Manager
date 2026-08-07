package com.example.alcazar.security

import org.json.JSONObject
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class BackupEngine {

    private val PBKDF2_ITERATIONS = 100_000
    private val KEY_LENGTH = 128

    fun generateRecoveryKey(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
        return (1..32).map { chars.random() }.joinToString("")
    }

    private fun deriveKey(secret: String, salt: ByteArray): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(secret.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH)
        return SecretKeySpec(factory.generateSecret(spec).encoded, "AES")
    }

    fun createEnvelopeBackup(payloadJson: String, masterPassword: String, recoveryKey: String): String {
        val random = SecureRandom()

        // 1. Generate VMK (16 bytes for AES-128)
        val vmkBytes = ByteArray(16)
        random.nextBytes(vmkBytes)
        val vmk = SecretKeySpec(vmkBytes, "AES")

        // 2. Encrypt Payload
        val payloadCipher = Cipher.getInstance("AES/GCM/NoPadding")
        payloadCipher.init(Cipher.ENCRYPT_MODE, vmk)
        val payloadIv = payloadCipher.iv
        val encryptedPayload = payloadCipher.doFinal(payloadJson.toByteArray(Charsets.UTF_8))

        // 3. Wrap VMK with Master Password
        val masterSalt = ByteArray(16).apply { random.nextBytes(this) }
        val masterDerivedKey = deriveKey(masterPassword, masterSalt)
        val masterWrapCipher = Cipher.getInstance("AES/GCM/NoPadding")
        masterWrapCipher.init(Cipher.ENCRYPT_MODE, masterDerivedKey)
        val masterWrapIv = masterWrapCipher.iv
        val wrappedVmkByMaster = masterWrapCipher.doFinal(vmkBytes)

        // 4. Wrap VMK with Recovery Key
        val recoverySalt = ByteArray(16).apply { random.nextBytes(this) }
        val recoveryDerivedKey = deriveKey(recoveryKey, recoverySalt)
        val recoveryWrapCipher = Cipher.getInstance("AES/GCM/NoPadding")
        recoveryWrapCipher.init(Cipher.ENCRYPT_MODE, recoveryDerivedKey)
        val recoveryWrapIv = recoveryWrapCipher.iv
        val wrappedVmkByRecovery = recoveryWrapCipher.doFinal(vmkBytes)

        // 5. Structure JSON
        val backupJson = JSONObject()
        backupJson.put("version", 1)

        val masterData = JSONObject()
        masterData.put("salt", android.util.Base64.encodeToString(masterSalt, android.util.Base64.NO_WRAP))
        masterData.put("iv", android.util.Base64.encodeToString(masterWrapIv, android.util.Base64.NO_WRAP))
        masterData.put("wrapped_key", android.util.Base64.encodeToString(wrappedVmkByMaster, android.util.Base64.NO_WRAP))
        backupJson.put("master_envelope", masterData)

        val recoveryData = JSONObject()
        recoveryData.put("salt", android.util.Base64.encodeToString(recoverySalt, android.util.Base64.NO_WRAP))
        recoveryData.put("iv", android.util.Base64.encodeToString(recoveryWrapIv, android.util.Base64.NO_WRAP))
        recoveryData.put("wrapped_key", android.util.Base64.encodeToString(wrappedVmkByRecovery, android.util.Base64.NO_WRAP))
        backupJson.put("recovery_envelope", recoveryData)

        backupJson.put("payload_iv", android.util.Base64.encodeToString(payloadIv, android.util.Base64.NO_WRAP))
        backupJson.put("encrypted_payload", android.util.Base64.encodeToString(encryptedPayload, android.util.Base64.NO_WRAP))

        return backupJson.toString()
    }

    fun restoreFromEnvelope(backupJsonString: String, secretProvided: String, isRecoveryKey: Boolean): String? {
        return try {
            val backupJson = JSONObject(backupJsonString)
            val envelope = if (isRecoveryKey) backupJson.getJSONObject("recovery_envelope") else backupJson.getJSONObject("master_envelope")

            val salt = android.util.Base64.decode(envelope.getString("salt"), android.util.Base64.NO_WRAP)
            val wrapIv = android.util.Base64.decode(envelope.getString("iv"), android.util.Base64.NO_WRAP)
            val wrappedVmk = android.util.Base64.decode(envelope.getString("wrapped_key"), android.util.Base64.NO_WRAP)

            val derivedKey = deriveKey(secretProvided, salt)

            val unwrapCipher = Cipher.getInstance("AES/GCM/NoPadding")
            unwrapCipher.init(Cipher.DECRYPT_MODE, derivedKey, GCMParameterSpec(128, wrapIv))
            val vmkBytes = unwrapCipher.doFinal(wrappedVmk)
            val vmk = SecretKeySpec(vmkBytes, "AES")

            val payloadIv = android.util.Base64.decode(backupJson.getString("payload_iv"), android.util.Base64.NO_WRAP)
            val encryptedPayload = android.util.Base64.decode(backupJson.getString("encrypted_payload"), android.util.Base64.NO_WRAP)

            val payloadCipher = Cipher.getInstance("AES/GCM/NoPadding")
            payloadCipher.init(Cipher.DECRYPT_MODE, vmk, GCMParameterSpec(128, payloadIv))
            val decryptedBytes = payloadCipher.doFinal(encryptedPayload)

            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}