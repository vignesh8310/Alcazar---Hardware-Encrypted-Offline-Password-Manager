package com.example.alcazar.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.alcazar.data.repository.VaultRepository
import com.example.alcazar.security.CryptoManager
import com.example.alcazar.security.PrefsManager

class VaultViewModelFactory(
    private val repository: VaultRepository,
    private val cryptoManager: CryptoManager,
    private val prefsManager: PrefsManager
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VaultViewModel::class.java)) {
            return VaultViewModel(repository, cryptoManager, prefsManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}