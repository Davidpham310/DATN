package com.example.datn.core.utils.validation

object AllowedSubjects {
    val allowedSubjectsList: List<String> = listOf(
        "Toán", "Ngữ văn", "Tiếng Anh", "Vật lý",
        "Hóa học", "Sinh học", "Lịch sử",
        "Địa lý", "GDCD", "Tin học", "Công nghệ"
    )

    val allowedSubjects: Set<String> = allowedSubjectsList.toSet()
}
