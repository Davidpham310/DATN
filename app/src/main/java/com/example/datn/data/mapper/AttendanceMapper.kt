package com.example.datn.data.mapper

import com.example.datn.data.local.entities.AttendanceEntity
import com.example.datn.domain.models.Attendance

fun AttendanceEntity.toDomain(): Attendance {
    return Attendance(
        id = id,
        classId = classId,
        studentId = studentId,
        date = date,
        status = status,
        note = note
    )
}

fun Attendance.toEntity(): AttendanceEntity {
    return AttendanceEntity(
        id = id,
        classId = classId,
        studentId = studentId,
        date = date,
        status = status,
        note = note
    )
}
