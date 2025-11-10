package com.example.datn.presentation.student.tests

/**
 * Sealed class đại diện cho các loại câu trả lời
 * Sử dụng chung cho cả StudentTestTaking và TestResult
 */
sealed class Answer {
    /**
     * Câu trả lời trắc nghiệm 1 đáp án
     * @param optionId ID của option được chọn
     */
    data class SingleChoice(val optionId: String) : Answer()
    
    /**
     * Câu trả lời trắc nghiệm nhiều đáp án
     * @param optionIds Set các ID của options được chọn
     */
    data class MultipleChoice(val optionIds: Set<String>) : Answer()
    
    /**
     * Câu trả lời điền vào chỗ trống
     * @param text Nội dung text của câu trả lời
     */
    data class FillBlank(val text: String) : Answer()
    
    /**
     * Câu trả lời tự luận
     * @param text Nội dung bài luận
     */
    data class Essay(val text: String) : Answer()
}
