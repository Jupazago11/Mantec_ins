package com.example.mantec_ins.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.mantec_ins.data.repository.CatalogLocalRepository
import com.example.mantec_ins.data.repository.RemoteCatalogRepository

class CatalogViewModelFactory(
    private val repository: CatalogLocalRepository,
    private val remoteCatalogRepository: RemoteCatalogRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CatalogViewModel::class.java)) {
            return CatalogViewModel(
                repository = repository,
                remoteCatalogRepository = remoteCatalogRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}