package com.example.mantec_ins.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.mantec_ins.data.repository.AuthRepository
import com.example.mantec_ins.data.repository.RemoteCatalogRepository

class LoginViewModelFactory(
    private val authRepository: AuthRepository,
    private val remoteCatalogRepository: RemoteCatalogRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            return LoginViewModel(authRepository, remoteCatalogRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
