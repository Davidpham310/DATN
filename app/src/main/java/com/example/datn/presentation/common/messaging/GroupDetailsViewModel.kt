package com.example.datn.presentation.common.messaging

import androidx.lifecycle.viewModelScope
import com.example.datn.core.base.BaseViewModel
import com.example.datn.core.utils.Resource
import com.example.datn.domain.usecase.auth.AuthUseCases
import com.example.datn.domain.usecase.messaging.LeaveGroupParams
import com.example.datn.domain.usecase.messaging.MessagingUseCases
import com.example.datn.presentation.common.notifications.NotificationManager
import com.example.datn.presentation.common.notifications.NotificationType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GroupDetailsViewModel @Inject constructor(
    private val messagingUseCases: MessagingUseCases,
    private val authUseCases: AuthUseCases,
    notificationManager: NotificationManager
) : BaseViewModel<GroupDetailsState, GroupDetailsEvent>(GroupDetailsState(), notificationManager) {

    private val currentUserIdFlow: StateFlow<String> = authUseCases.getCurrentIdUser.invoke()
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = ""
        )

    override fun onEvent(event: GroupDetailsEvent) {
        when (event) {
            is GroupDetailsEvent.LoadParticipants -> loadParticipants(event.conversationId)
            is GroupDetailsEvent.LeaveGroup -> leaveGroup(event.conversationId)
        }
    }

    fun loadParticipants(conversationId: String) {
        viewModelScope.launch {
            val currentUserId = currentUserIdFlow.value.ifBlank {
                currentUserIdFlow.filter { it.isNotBlank() }.first()
            }
            setState { copy(currentUserId = currentUserId) }

            messagingUseCases.getGroupParticipants(conversationId)
                .onEach { result ->
                    when (result) {
                        is Resource.Loading -> {
                            setState { copy(isLoading = true, error = null) }
                        }
                        is Resource.Success -> {
                            setState { 
                                copy(
                                    isLoading = false,
                                    participants = result.data ?: emptyList()
                                ) 
                            }
                        }
                        is Resource.Error -> {
                            setState { 
                                copy(
                                    isLoading = false, 
                                    error = result.message ?: "Không thể tải danh sách thành viên"
                                ) 
                            }
                        }
                    }
                }
                .launchIn(viewModelScope)
        }
    }

    fun leaveGroup(conversationId: String) {
        viewModelScope.launch {
            val currentUserId = currentUserIdFlow.value.ifBlank {
                currentUserIdFlow.filter { it.isNotBlank() }.first()
            }

            messagingUseCases.leaveGroup(
                LeaveGroupParams(
                    conversationId = conversationId,
                    userId = currentUserId
                )
            ).onEach { result ->
                when (result) {
                    is Resource.Loading -> {
                        setState { copy(isLoading = true) }
                    }
                    is Resource.Success -> {
                        showNotification("Đã rời khỏi nhóm", NotificationType.SUCCESS)
                        setState { copy(isLoading = false, hasLeftGroup = true) }
                    }
                    is Resource.Error -> {
                        setState { copy(isLoading = false) }
                        showNotification(
                            result.message ?: "Không thể rời khỏi nhóm",
                            NotificationType.ERROR
                        )
                    }
                }
            }.launchIn(viewModelScope)
        }
    }
}
