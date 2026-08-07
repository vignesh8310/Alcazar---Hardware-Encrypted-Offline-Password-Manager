# Alcázar 🛡️

**Alcázar** is a zero-knowledge, offline security application for Android designed to store passwords, sensitive credentials, and encrypted notes without relying on cloud servers or remote third-party databases.

---

## 🔒 Key Features

- **🛡️ Duress/Decoy Vault:** Dual-vault architecture (Real + Fake). Entering the Master Key opens your real data, while entering a secondary Duress Key opens an empty decoy vault for built-in plausible deniability.
- **🔐 Hardware-Backed Encryption:** All credentials and secure notes are encrypted using AES-256-GCM backed by the native Android KeyStore (`CryptoManager`) in tamper-resistant hardware.
- **📡 Offline-First & Zero-Knowledge:** Zero internet permission (`AndroidManifest.xml`). No cloud, no remote servers, and no background sync—your data never leaves your device.
- **💾 Encrypted Backup & Recovery Key:** Export fully encrypted local backups secured with a 32-character Recovery Key for two-key protection.
- **🔄 Cross-Device Restore:** Effortlessly transfer and migrate your encrypted vault file across any Android device running Alcázar.
- **🖱️ Mass Delete & Bulk Actions:** Long-press selection allows multi-item selection and instant bulk deletion for seamless management.
- **🏷️ Smart Categories:** Organize passwords and credentials with custom tags (Work, Finance, Personal, Crypto) for fast retrieval.
- **🔑 Built-in Password Generator:** Generate strong, customizable 16-character random passwords with a single tap.
- **📝 Encrypted Secure Notes:** Dedicated local storage for private text, secrets, recovery seeds, and sensitive documentation behind encryption layers.
- **🔒 Root Detection & Anti-Screenshot Protection:** Enforces screen recording prevention (`FLAG_SECURE`), auto-clears sensitive clipboard data, and flags rooted environments for maximum execution security.

---

## 🏗️ Architecture & Technology Stack

- **Language:** Kotlin
- **UI Framework:** Jetpack Compose (Material 3)
- **Architecture Pattern:** MVVM (Model-View-ViewModel)
- **Database:** Room Database
- **Security Primitives:** Android KeyStore, EncryptedSharedPreferences, AES-256-GCM
- **Navigation:** Jetpack Navigation Compose

---

## 📂 Project Structure

```text
app/src/main/java/com/example/alcazar/
├── MainActivity.kt          # Application entry point
├── data/                    # Room Database, DAOs, Entities, and Repository
├── security/                # CryptoManager, PrefsManager, and BackupEngine
├── navigation/              # NavHost configuration and Screen routes
├── ui/                      # Jetpack Compose Screens, Components, and Theme
└── viewmodel/               # VaultViewModel and ViewModelFactory
