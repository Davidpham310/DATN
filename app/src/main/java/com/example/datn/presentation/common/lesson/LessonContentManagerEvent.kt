package com.example.datn.presentation.common.lesson

import com.example.datn.core.base.BaseEvent
import com.example.datn.domain.models.LessonContent
import java.io.InputStream

sealed class LessonContentManagerEvent : BaseEvent {
    object RefreshContents : LessonContentManagerEvent()
    data class LoadContentsForLesson(val lessonId: String) : LessonContentManagerEvent()
    data class SelectContent(val content: LessonContent) : LessonContentManagerEvent()

    // Dialog actions
    object ShowAddContentDialog : LessonContentManagerEvent()
    data class EditContent(val content: LessonContent) : LessonContentManagerEvent()
    data class DeleteContent(val content: LessonContent) : LessonContentManagerEvent()
    object DismissDialog : LessonContentManagerEvent()


    // CRUD operations
    data class ConfirmAddContent(
        val lessonId: String,
        val title: String,
        val description: String?,
        val contentLink: String?,
        val contentType: String,
        val fileStream: InputStream? = null,
        val fileSize: Long = 0,
    ) : LessonContentManagerEvent()

    data class ConfirmEditContent(
        val id: String,
        val lessonId: String,
        val title: String,
        val description: String?,
        val contentLink: String?,
        val contentType: String,
        val fileStream: InputStream? = null,
        val fileSize: Long = 0,
    ) : LessonContentManagerEvent()
}