package com.example.alcazar.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alcazar.data.model.NoteEntry
import com.example.alcazar.data.model.PasswordEntry
import com.example.alcazar.data.repository.VaultRepository
import com.example.alcazar.security.BackupEngine
import com.example.alcazar.security.CryptoManager
import com.example.alcazar.security.PrefsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream

class VaultViewModel(
    private val repository: VaultRepository,
    private val cryptoManager: CryptoManager,
    private val prefsManager: PrefsManager
) : ViewModel() {

    private val backupEngine = BackupEngine()

    // --- Error State ---
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun clearError() { _errorMessage.value = null }

    // --- Identity & Username State ---
    private val _userName = MutableStateFlow(prefsManager.getUsername())
    val userName: StateFlow<String> = _userName.asStateFlow()

    fun updateUsername(newUsername: String) {
        prefsManager.saveUsername(newUsername)
        _userName.value = newUsername
    }

    fun refreshUsername() {
        _userName.value = prefsManager.getUsername()
    }

    // --- Session & Mode ---
    private val _isDuressMode = MutableStateFlow(false)
    val isDuressMode: StateFlow<Boolean> = _isDuressMode.asStateFlow()

    // --- Data from Room ---
    private val _passwords = MutableStateFlow<List<PasswordEntry>>(emptyList())
    val allPasswords: StateFlow<List<PasswordEntry>> = _passwords.asStateFlow()

    private val _notes = MutableStateFlow<List<NoteEntry>>(emptyList())
    val allNotes: StateFlow<List<NoteEntry>> = _notes.asStateFlow()

    // --- Categories ---
    private val _dynamicCategories = MutableStateFlow(listOf("All", "General", "Personal", "Work", "Finance", "Social", "Crypto"))
    val dynamicCategories: StateFlow<List<String>> = _dynamicCategories.asStateFlow()

    init {
        loadAllData()
        refreshUsername()
    }

    private fun loadAllData() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.getAllPasswords(_isDuressMode.value).collect { list ->
                    _passwords.value = list
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _errorMessage.value = "Failed to load passwords: ${e.localizedMessage}"
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.getAllNotes(_isDuressMode.value).collect { list ->
                    _notes.value = list
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _errorMessage.value = "Failed to load notes: ${e.localizedMessage}"
            }
        }
        updateCategories()
    }

    // --- Session management (overloads) ---
    fun setVaultSession(isDuress: Boolean = false, vararg extra: Any?) {
        _isDuressMode.value = isDuress
        refreshUsername()
        loadAllData()
    }

    fun setVaultSession(sessionMode: String = "REAL", vararg extra: Any?) {
        _isDuressMode.value = sessionMode.equals("DURESS", ignoreCase = true)
        refreshUsername()
        loadAllData()
    }

    // --- Encryption helpers with error handling ---
    fun encryptSecret(plainText: String): String {
        if (plainText.isBlank()) return ""
        return try {
            cryptoManager.encrypt(
                plainText,
                if (_isDuressMode.value) cryptoManager.ALIAS_DURESS else cryptoManager.ALIAS_REAL
            )
        } catch (e: Exception) {
            e.printStackTrace()
            _errorMessage.value = "Encryption failed: ${e.localizedMessage}"
            plainText // fallback – but we show error
        }
    }

    fun decryptSecret(cipherText: String, vararg extra: Any?): String {
        if (cipherText.isBlank()) return ""
        return try {
            val alias = if (_isDuressMode.value) cryptoManager.ALIAS_DURESS else cryptoManager.ALIAS_REAL
            cryptoManager.decrypt(cipherText, alias)
        } catch (e: Exception) {
            e.printStackTrace()
            _errorMessage.value = "Decryption failed: ${e.localizedMessage}"
            cipherText
        }
    }

    // --- Authentication ---
    fun verifyPasscode(input: String): String {
        return try {
            val master = prefsManager.getMasterKey()
            val duress = prefsManager.getDuressKey()
            when {
                input == master -> "REAL"
                input == duress -> "DURESS"
                else -> "INVALID"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _errorMessage.value = "Verification failed: ${e.localizedMessage}"
            "INVALID"
        }
    }

    fun updateMasterKey(newKey: String) {
        try {
            prefsManager.saveMasterKey(newKey)
        } catch (e: Exception) {
            e.printStackTrace()
            _errorMessage.value = "Failed to update master key: ${e.localizedMessage}"
        }
    }

    fun generateNewRecoveryKey(): String {
        return try {
            backupEngine.generateRecoveryKey()
        } catch (e: Exception) {
            e.printStackTrace()
            _errorMessage.value = "Failed to generate recovery key: ${e.localizedMessage}"
            ""
        }
    }

    // --- Password CRUD ---
    fun insertPassword(entry: PasswordEntry) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val encrypted = entry.copy(
                    encryptedPassword = encryptSecret(entry.encryptedPassword),
                    isDuress = _isDuressMode.value
                )
                repository.insertPassword(encrypted)
                updateCategories()
            } catch (e: Exception) {
                e.printStackTrace()
                _errorMessage.value = "Failed to save password: ${e.localizedMessage}"
            }
        }
    }

    fun addPassword(entry: PasswordEntry) = insertPassword(entry)

    fun addPassword(title: String, username: String, pass: String, category: String = "General") {
        insertPassword(
            PasswordEntry(
                title = title,
                username = username,
                encryptedPassword = pass,
                category = category,
                isDuress = _isDuressMode.value
            )
        )
    }

    fun updatePassword(entry: PasswordEntry) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val encrypted = entry.copy(
                    encryptedPassword = encryptSecret(entry.encryptedPassword)
                )
                repository.updatePassword(encrypted)
                updateCategories()
            } catch (e: Exception) {
                e.printStackTrace()
                _errorMessage.value = "Failed to update password: ${e.localizedMessage}"
            }
        }
    }

    fun deletePassword(entry: PasswordEntry) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.deletePassword(entry)
                updateCategories()
            } catch (e: Exception) {
                e.printStackTrace()
                _errorMessage.value = "Failed to delete password: ${e.localizedMessage}"
            }
        }
    }

    // --- Note CRUD ---
    fun insertNote(entry: NoteEntry) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val encrypted = entry.copy(
                    encryptedContent = encryptSecret(entry.encryptedContent),
                    isDuress = _isDuressMode.value
                )
                repository.insertNote(encrypted)
            } catch (e: Exception) {
                e.printStackTrace()
                _errorMessage.value = "Failed to save note: ${e.localizedMessage}"
            }
        }
    }

    fun addNote(entry: NoteEntry) = insertNote(entry)

    fun addNote(title: String, content: String) {
        insertNote(
            NoteEntry(
                title = title,
                encryptedContent = content,
                isDuress = _isDuressMode.value
            )
        )
    }

    fun updateNote(entry: NoteEntry) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val encrypted = entry.copy(
                    encryptedContent = encryptSecret(entry.encryptedContent)
                )
                repository.updateNote(encrypted)
            } catch (e: Exception) {
                e.printStackTrace()
                _errorMessage.value = "Failed to update note: ${e.localizedMessage}"
            }
        }
    }

    fun deleteNote(entry: NoteEntry) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.deleteNote(entry)
            } catch (e: Exception) {
                e.printStackTrace()
                _errorMessage.value = "Failed to delete note: ${e.localizedMessage}"
            }
        }
    }

    // --- Category updater ---
    private fun updateCategories() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val all = repository.getAllPasswordsSync(false)
                val dbCategories = all.map { it.category }.distinct().sorted()
                val defaults = listOf("All", "General", "Personal", "Work", "Finance", "Social", "Crypto")
                val combined = (defaults + dbCategories).distinct()
                _dynamicCategories.value = combined
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // --- Backup / Restore ---
    suspend fun exportVaultForMigration(
        password: String,
        recoveryKey: String,
        outputStream: OutputStream
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject()

            // Export passwords (only real ones, not duress)
            val passArray = JSONArray()
            repository.getAllPasswordsSync(false).forEach { p ->
                passArray.put(
                    JSONObject().apply {
                        put("title", p.title)
                        put("username", p.username)
                        put("encryptedPassword", p.encryptedPassword)
                        put("category", p.category)
                    }
                )
            }
            root.put("passwords", passArray)

            // Export notes (only real ones, not duress)
            val noteArray = JSONArray()
            repository.getAllNotesSync(false).forEach { n ->
                noteArray.put(
                    JSONObject().apply {
                        put("title", n.title)
                        put("encryptedContent", n.encryptedContent)
                    }
                )
            }
            root.put("notes", noteArray)

            val encrypted = backupEngine.createEnvelopeBackup(root.toString(), password, recoveryKey)
            outputStream.write(encrypted.toByteArray(Charsets.UTF_8))
            outputStream.flush()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            _errorMessage.value = "Export failed: ${e.localizedMessage}"
            false
        }
    }

    suspend fun importVaultFromMigration(
        secret: String,
        isRecoveryKey: Boolean,
        inputStream: InputStream
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val raw = inputStream.bufferedReader().use { it.readText() }
            val decrypted = backupEngine.restoreFromEnvelope(raw, secret, isRecoveryKey)
                ?: return@withContext false
            val root = JSONObject(decrypted)

            // Import passwords
            root.optJSONArray("passwords")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val entry = PasswordEntry(
                        title = obj.optString("title", "Imported"),
                        username = obj.optString("username", ""),
                        encryptedPassword = obj.optString("encryptedPassword", ""),
                        category = obj.optString("category", "General"),
                        isDuress = false
                    )
                    val encrypted = entry.copy(encryptedPassword = encryptSecret(entry.encryptedPassword))
                    repository.insertPassword(encrypted)
                }
            }

            // Import notes
            root.optJSONArray("notes")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val entry = NoteEntry(
                        title = obj.optString("title", "Imported"),
                        encryptedContent = obj.optString("encryptedContent", ""),
                        isDuress = false
                    )
                    val encrypted = entry.copy(encryptedContent = encryptSecret(entry.encryptedContent))
                    repository.insertNote(encrypted)
                }
            }

            updateCategories()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            _errorMessage.value = "Import failed: ${e.localizedMessage}"
            false
        }
    }
    fun getThemeMode(): Int = prefsManager.getThemeMode()
    fun setThemeMode(mode: Int) = prefsManager.setThemeMode(mode)
}