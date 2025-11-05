package com.example.datn.presentation.common.messaging

import androidx.lifecycle.viewModelScope
import com.example.datn.core.base.BaseViewModel
import com.example.datn.core.utils.Resource
import com.example.datn.domain.usecase.messaging.AddParticipantsParams
import com.example.datn.domain.usecase.messaging.MessagingUseCases
import com.example.datn.presentation.common.notifications.NotificationManager
import com.example.datn.presentation.common.notifications.NotificationType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddMembersViewModel @Inject constructor(
    private val messagingUseCases: MessagingUseCases,
    notificationManager: NotificationManager
) : BaseViewModel<AddMembersState, AddMembersEvent>(AddMembersState(), notificationManager) {

    override fun onEvent(event: AddMembersEvent) {
        when (event) {
            is AddMembersEvent.AddMembers -> addMembers(event.conversationId, event.userIds)
        }
    }

    fun addMembers(conversationId: String, userIds: List<String>) {
        viewModelScope.launch {
            messagingUseCases.addParticipants(
                AddParticipantsParams(
                    conversationId = conversationId,
                    userIds = userIds
                )
            ).onEach { result ->
                when (result) {
                    is Resource.Loading -> {
                        setState { copy(isLoading = true, error = null) }
                    }
                    is Resource.Success -> {
                        showNotification("Đã thêm ${userIds.size} thành viên", NotificationType.SUCCESS)
                        setState { copy(isLoading = false, isSuccess = true) }
                    }
                    is Resource.Error -> {
                        setState { copy(isLoading = false, error = result.message) }
                        showNotification(
                            result.message ?: "Không thể thêm thành viên",
                            NotificationType.ERROR
                        )
                    }
                }
            }.launchIn(viewModelScope)
        }
    }
}
