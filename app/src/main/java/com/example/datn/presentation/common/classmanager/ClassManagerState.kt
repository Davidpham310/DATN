package com.example.datn.presentation.common.classmanager

import com.example.datn.core.base.BaseState
import com.example.datn.domain.models.Class

data class ClassManagerState(
    override val isLoading: Boolean = false,
    override val error: String? = null,
    val classes: List<Class> = emptyList(),
    val selectedClass: Class? = null,
    val showAddEditDialog: Boolean = false,
    val editingClass: Class? = null
) : BaseState