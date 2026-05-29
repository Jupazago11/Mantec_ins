package com.example.mantec_ins.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.example.mantec_ins.data.local.ElementTypeEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class InspectorProfileUiState(
    val userId: Long = 0L,
    val userName: String = "",
    val username: String = "",

    val roleKey: String? = null,

    val clientId: Long = 0L,
    val clientName: String = "",

    val groupId: Long? = null,
    val groupName: String = "",
    val groupDescription: String? = null,
    val groupAutoSync: Boolean = false,

    val specialtyId: Long? = null,
    val specialtyName: String = "",
    val availableElementTypes: List<ElementTypeEntity> = emptyList()
)

class InspectorProfileViewModel : ViewModel() {

    private val _profile = MutableStateFlow(InspectorProfileUiState())
    val profile: StateFlow<InspectorProfileUiState> = _profile

    fun setProfile(
        userId: Long,
        userName: String,
        username: String,
        roleKey: String?,
        clientId: Long,
        clientName: String,
        groupId: Long?,
        groupName: String,
        groupDescription: String?,
        groupAutoSync: Boolean,
        specialtyId: Long?,
        specialtyName: String,
        availableElementTypes: List<ElementTypeEntity> = emptyList()
    ) {
        _profile.value = InspectorProfileUiState(
            userId = userId,
            userName = userName,
            username = username,
            roleKey = roleKey,
            clientId = clientId,
            clientName = clientName,
            groupId = groupId,
            groupName = groupName,
            groupDescription = groupDescription,
            groupAutoSync = groupAutoSync,
            specialtyId = specialtyId,
            specialtyName = specialtyName,
            availableElementTypes = availableElementTypes
        )
    }

    fun setSelectedSpecialty(
        specialtyId: Long,
        specialtyName: String
    ) {
        _profile.value = _profile.value.copy(
            specialtyId = specialtyId,
            specialtyName = specialtyName
        )
    }

    fun setAvailableElementTypes(
        elementTypes: List<ElementTypeEntity>
    ) {
        val current = _profile.value

        val selected = when {
            current.specialtyId != null && elementTypes.any { it.id == current.specialtyId } ->
                elementTypes.first { it.id == current.specialtyId }

            elementTypes.size == 1 ->
                elementTypes.first()

            else -> null
        }

        _profile.value = current.copy(
            availableElementTypes = elementTypes,
            specialtyId = selected?.id,
            specialtyName = selected?.name ?: ""
        )
    }

    fun clearProfile() {
        _profile.value = InspectorProfileUiState()
    }
}