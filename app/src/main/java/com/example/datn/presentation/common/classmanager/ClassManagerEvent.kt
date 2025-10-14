package com.example.datn.presentation.common.classmanager

import com.example.datn.core.base.BaseEvent
import com.example.datn.domain.models.Class

sealed class ClassManagerEvent : BaseEvent {
    object RefreshClasses : ClassManagerEvent()
    data class SelectClass(val classModel: Class) : ClassManagerEvent()
    data class DeleteClass(val classModel: Class) : ClassManagerEvent()
    data class ShowError(val message: String) : ClassManagerEvent()
    object ShowAddClassDialog : ClassManagerEvent()

    data class ConfirmAddClass(val name: String, val classCode: String) : ClassManagerEvent()
    data class EditClass(val classModel: Class) : ClassManagerEvent()
    object DismissDialog : ClassManagerEvent()
}
