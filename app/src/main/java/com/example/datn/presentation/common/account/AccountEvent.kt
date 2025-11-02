package com.example.datn.presentation.common.account

import com.example.datn.core.base.BaseEvent

sealed class AccountEvent : BaseEvent {
    object LoadCurrentUser : AccountEvent()
    object SignOut : AccountEvent()
    object ClearMessages : AccountEvent()
}
