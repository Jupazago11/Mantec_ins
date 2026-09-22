package com.example.mantec_ins.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.mantec_ins.data.repository.PersonalActivityLocalRepository
import com.example.mantec_ins.data.repository.PersonalActivityRepository
import com.example.mantec_ins.data.repository.SupervisorSyncRepository

class SupervisorActivityViewModelFactory(
    private val localRepository: PersonalActivityLocalRepository,
    private val syncRepository: SupervisorSyncRepository,
    private val remoteRepository: PersonalActivityRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SupervisorActivityViewModel::class.java)) {
            return SupervisorActivityViewModel(localRepository, syncRepository, remoteRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
