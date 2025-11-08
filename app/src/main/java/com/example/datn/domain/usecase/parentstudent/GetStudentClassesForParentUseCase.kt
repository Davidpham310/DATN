package com.example.datn.domain.usecase.parentstudent

import com.example.datn.core.utils.Resource
import com.example.datn.domain.models.ClassEnrollmentInfo
import com.example.datn.domain.models.EnrollmentStatus
import com.example.datn.domain.repository.IParentRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use Case để lấy danh sách lớp học mà con của phụ huynh đang tham gia.
 * 
 * Chức năng:
 * - Lấy tất cả lớp học mà con của phụ huynh đang tham gia hoặc có liên quan
 * - Hỗ trợ filter theo học sinh cụ thể
 * - Hỗ trợ filter theo trạng thái enrollment (APPROVED, PENDING, REJECTED, WITHDRAWN)
 * - Tự động sắp xếp: APPROVED trước, PENDING sau, REJECTED/WITHDRAWN cuối
 * - Trong cùng trạng thái, sắp xếp theo ngày tham gia mới nhất trước
 * 
 * Edge cases được xử lý:
 * - Phụ huynh chưa có con: Trả về list rỗng
 * - Con chưa tham gia lớp nào: Trả về list rỗng
 * - Giáo viên bị xóa/không active: Hiển thị "(Đã rời)"
 * - Class bị xóa: Không hiển thị (filter)
 */
class GetStudentClassesForParentUseCase @Inject constructor(
    private val parentRepository: IParentRepository
) {
    /**
     * @param parentId ID phụ huynh hiện tại
     * @param studentId ID học sinh (optional - lọc theo con cụ thể)
     * @param enrollmentStatus Trạng thái enrollment (optional - lọc theo trạng thái)
     * @return Flow<Resource<List<ClassEnrollmentInfo>>>
     */
    operator fun invoke(
        parentId: String,
        studentId: String? = null,
        enrollmentStatus: EnrollmentStatus? = null
    ): Flow<Resource<List<ClassEnrollmentInfo>>> {
        return parentRepository.getStudentClassesForParent(
            parentId = parentId,
            studentId = studentId,
            enrollmentStatus = enrollmentStatus
        )
    }
}
