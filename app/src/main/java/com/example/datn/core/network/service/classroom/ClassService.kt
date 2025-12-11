package com.example.datn.core.network.service.classroom

import android.util.Log
import com.example.datn.core.network.service.firestore.BaseFirestoreService
import com.example.datn.core.utils.mapper.internalToDomain
import com.example.datn.domain.models.Class
import com.example.datn.domain.models.ClassStudent
import com.example.datn.domain.models.EnrollmentStatus
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.time.Instant
import javax.inject.Inject

private const val TAG = "ClassService"

class ClassService @Inject constructor() :
    BaseFirestoreService<Class>(
        collectionName = "classes",
        clazz = Class::class.java
    ) {

    private val classStudentRef = FirebaseFirestore.getInstance().collection("class_students")

    // ==================== CLASS OPERATIONS ====================

    /**
     * Lấy lớp theo ID
     */
    suspend fun getClassById(classId: String): Class? {
        Log.d(TAG, "Fetching class by ID: $classId")
        return try {
            val doc = collectionRef.document(classId).get().await()
            if (doc.exists()) {
                val classObj = doc.internalToDomain(clazz)
                Log.i(TAG, "Successfully fetched class: ${classObj?.name} (ID: $classId)")
                classObj
            } else {
                Log.w(TAG, "Class document not found for ID: $classId")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching class by ID: $classId", e)
            null
        }
    }

    /**
     * Tìm kiếm lớp theo mã lớp (classCode)
     */
    suspend fun getClassByCode(classCode: String): Class? {
        Log.d(TAG, "Fetching class by code: $classCode")
        return try {
            val snapshot = collectionRef
                .whereEqualTo("classCode", classCode)
                .limit(1)
                .get()
                .await()

            if (snapshot.isEmpty) {
                Log.w(TAG, "Class not found with code: $classCode")
                null
            } else {
                val classObj = snapshot.documents.first().internalToDomain(clazz)
                Log.i(TAG, "Successfully fetched class: ${classObj?.name} (Code: $classCode)")
                classObj
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching class by code: $classCode", e)
            null
        }
    }

    /**
     * Lấy tất cả lớp của giáo viên
     */
    suspend fun getClassesByTeacher(teacherId: String): List<Class> {
        Log.d(TAG, "Fetching classes for teacher: $teacherId")
        return try {
            val snapshot = collectionRef
                .whereEqualTo("teacherId", teacherId)
                .get()
                .await()

            snapshot.documents.forEach { doc ->
                Log.d(TAG, "Raw Firestore data: ${doc.data}")
            }

            val classes = snapshot.documents.mapNotNull {
                try {
                    it.internalToDomain(clazz)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse doc ${it.id}", e)
                    null
                }
            }

            Log.d(TAG, "Mapped ${classes.size} classes for teacher $teacherId")
            classes
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching classes by teacher: $teacherId", e)
            emptyList()
        }
    }

    /**
     * Lấy tất cả enrollments của học sinh từ Firebase (bất kỳ status nào)
     */
    suspend fun getEnrollmentsByStudent(
        studentId: String,
        enrollmentStatus: EnrollmentStatus? = null
    ): List<ClassStudent> {
        Log.d(TAG, "Fetching enrollments for student: $studentId, status: $enrollmentStatus")
        return try {
            var query = classStudentRef.whereEqualTo("studentId", studentId)
            
            // Filter theo status nếu có
            if (enrollmentStatus != null) {
                query = query.whereEqualTo("enrollmentStatus", enrollmentStatus.name)
            }
            
            val snapshot = query.get().await()
            
            val enrollments = snapshot.documents.mapNotNull { doc ->
                try {
                    ClassStudent(
                        classId = doc.getString("classId") ?: "",
                        studentId = doc.getString("studentId") ?: "",
                        enrollmentStatus = EnrollmentStatus.valueOf(
                            doc.getString("enrollmentStatus") ?: EnrollmentStatus.NOT_ENROLLED.name
                        ),
                        enrolledDate = doc.get("enrolledDate")?.let {
                            when (it) {
                                is com.google.firebase.Timestamp -> it.toDate().toInstant()
                                else -> Instant.now()
                            }
                        } ?: Instant.now(),
                        approvedBy = doc.getString("approvedBy") ?: "",
                        rejectionReason = doc.getString("rejectionReason") ?: ""
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse enrollment doc ${doc.id}", e)
                    null
                }
            }
            
            Log.d(TAG, "Found ${enrollments.size} enrollments for student $studentId")
            enrollments
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching enrollments for student: $studentId", e)
            emptyList()
        }
    }
    
    /**
     * Lấy tất cả lớp học sinh tham gia (status = APPROVED)
     */
    suspend fun getClassesByStudent(studentId: String): List<Class> {
        Log.d(TAG, "Fetching classes for student: $studentId")
        return try {
            // Lấy danh sách classId từ bảng class_students
            val classStudentSnapshot = classStudentRef
                .whereEqualTo("studentId", studentId)
                .whereEqualTo("enrollmentStatus", EnrollmentStatus.APPROVED.name)
                .get()
                .await()

            val classIds = classStudentSnapshot.documents.mapNotNull {
                it.getString("classId")
            }

            if (classIds.isEmpty()) {
                Log.d(TAG, "No classes found for student $studentId")
                return emptyList()
            }

            // Firestore chỉ hỗ trợ tối đa 10 items trong whereIn, cần chunk nếu nhiều hơn
            val classes = mutableListOf<Class>()
            classIds.chunked(10).forEach { chunk ->
                val snapshot = collectionRef
                    .whereIn("id", chunk)
                    .get()
                    .await()

                val chunkClasses = snapshot.documents.mapNotNull {
                    try {
                        it.internalToDomain(clazz)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to parse class doc ${it.id}", e)
                        null
                    }
                }
                classes.addAll(chunkClasses)
            }

            Log.d(TAG, "Found ${classes.size} classes for student $studentId")
            classes
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching classes by student: $studentId", e)
            emptyList()
        }
    }

    /**
     * Thêm lớp mới
     */
    suspend fun addClass(classObj: Class): Class? {
        Log.d(TAG, "Adding new class: ${classObj.name}")
        return try {
            val docRef = if (classObj.id.isNotEmpty()) {
                collectionRef.document(classObj.id)
            } else {
                collectionRef.document()
            }

            val now = Instant.now()
            val classWithId = classObj.copy(
                id = docRef.id,
                createdAt = now,
                updatedAt = now
            )

            docRef.set(classWithId).await()
            Log.i(TAG, "Successfully added class: ${classWithId.name} (ID: ${classWithId.id})")
            classWithId
        } catch (e: Exception) {
            Log.e(TAG, "Error adding class: ${classObj.name}", e)
            null
        }
    }

    /**
     * Cập nhật lớp
     */
    suspend fun updateClass(classId: String, classObj: Class): Boolean {
        Log.d(TAG, "Updating class: $classId")
        return try {
            val updatedClass = classObj.copy(
                id = classId,
                updatedAt = Instant.now()
            )
            collectionRef.document(classId).set(updatedClass).await()
            Log.i(TAG, "Successfully updated class: $classId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error updating class: $classId", e)
            false
        }
    }

    /**
     * Xóa lớp (và tất cả class_students liên quan)
     */
    suspend fun deleteClass(classId: String): Boolean {
        Log.d(TAG, "Deleting class: $classId")
        return try {
            // Xóa tất cả bản ghi trong class_students
            val classStudents = classStudentRef
                .whereEqualTo("classId", classId)
                .get()
                .await()

            // 2. Xóa bài học trong lớp
            val lessonRef = FirebaseFirestore.getInstance().collection("lessons")
            val lessons = lessonRef
                .whereEqualTo("classId", classId)
                .get()
                .await()


            firestore.runBatch { batch ->
                classStudents.documents.forEach { doc ->
                    batch.delete(doc.reference)
                }
                lessons.documents.forEach { batch.delete(it.reference) }
                batch.delete(collectionRef.document(classId))
            }.await()

            Log.i(TAG, "Successfully deleted class $classId and ${classStudents.size()} enrollments")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting class: $classId", e)
            false
        }
    }

    // ==================== CLASS-STUDENT OPERATIONS ====================

    /**
     * Thêm học sinh vào lớp (tạo enrollment request)
     */
    suspend fun addStudentToClass(
        classId: String,
        studentId: String,
        status: EnrollmentStatus = EnrollmentStatus.PENDING,
        approvedBy: String = "",
        rejectionReason: String = ""
    ): Boolean {
        Log.d(TAG, "Adding student $studentId to class $classId")
        return try {
            // Kiểm tra xem đã tồn tại chưa
            val existing = classStudentRef
                .whereEqualTo("classId", classId)
                .whereEqualTo("studentId", studentId)
                .get()
                .await()

            if (!existing.isEmpty) {
                Log.w(TAG, "Student $studentId already enrolled in class $classId")
                return false
            }

            val classStudent = ClassStudent(
                classId = classId,
                studentId = studentId,
                enrolledDate = Instant.now(),
                enrollmentStatus = status,
                approvedBy = approvedBy,
                rejectionReason = rejectionReason
            )

            classStudentRef.add(classStudent).await()
            Log.i(TAG, "Successfully added student $studentId to class $classId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error adding student to class", e)
            false
        }
    }

    /**
     * Xóa học sinh khỏi lớp
     */
    suspend fun removeStudentFromClass(classId: String, studentId: String): Boolean {
        Log.d(TAG, "Removing student $studentId from class $classId")
        return try {
            val snapshot = classStudentRef
                .whereEqualTo("classId", classId)
                .whereEqualTo("studentId", studentId)
                .get()
                .await()

            if (snapshot.isEmpty) {
                Log.w(TAG, "No enrollment found for student $studentId in class $classId")
                return false
            }

            snapshot.documents.forEach { it.reference.delete().await() }
            Log.i(TAG, "Successfully removed student $studentId from class $classId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error removing student from class", e)
            false
        }
    }

    /**
     * Cập nhật trạng thái enrollment (approve/reject)
     */
    suspend fun updateEnrollmentStatus(
        classId: String,
        studentId: String,
        status: EnrollmentStatus,
        approvedBy: String = "",
        rejectionReason: String = ""
    ): Boolean {
        Log.d(TAG, "Updating enrollment status for student $studentId in class $classId to $status")
        return try {
            val snapshot = classStudentRef
                .whereEqualTo("classId", classId)
                .whereEqualTo("studentId", studentId)
                .get()
                .await()

            if (snapshot.isEmpty) {
                Log.w(TAG, "No enrollment found")
                return false
            }

            val updates = mapOf(
                "enrollmentStatus" to status.name,
                "approvedBy" to approvedBy,
                "rejectionReason" to rejectionReason
            )

            snapshot.documents.first().reference.update(updates).await()
            Log.i(TAG, "Successfully updated enrollment status")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error updating enrollment status", e)
            false
        }
    }

    /**
     * Lấy danh sách học sinh trong lớp
     */
    suspend fun getStudentsInClass(
        classId: String,
        status: EnrollmentStatus? = null
    ): List<ClassStudent> {
        Log.d(TAG, "Fetching students in class $classId")
        return try {
            var query = classStudentRef.whereEqualTo("classId", classId)

            if (status != null) {
                query = query.whereEqualTo("enrollmentStatus", status.name)
            }

            val snapshot = query.get().await()

            snapshot.documents.mapNotNull {
                try {
                    it.internalToDomain(ClassStudent::class.java)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse ClassStudent doc ${it.id}", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching students in class", e)
            emptyList()
        }
    }

    /**
     * Lấy enrollment của một học sinh trong lớp cụ thể
     */
    suspend fun getEnrollment(classId: String, studentId: String): ClassStudent? {
        return try {
            val snapshot = classStudentRef
                .whereEqualTo("classId", classId)
                .whereEqualTo("studentId", studentId)
                .get()
                .await()

            if (snapshot.isEmpty) null
            else snapshot.documents.first().internalToDomain(ClassStudent::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching enrollment", e)
            null
        }
    }
}