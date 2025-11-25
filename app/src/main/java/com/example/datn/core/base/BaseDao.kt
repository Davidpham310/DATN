package com.example.datn.core.base

import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Update

/**
 * Dao cơ sở dùng chung cho các entity trong Room.
 * Cung cấp các hàm CRUD cơ bản:
 * - insert: chèn 1 bản ghi, nếu trùng khóa chính thì thay thế (REPLACE).
 * - insertAll: chèn danh sách bản ghi, cũng dùng chiến lược REPLACE khi xung đột.
 * - update: cập nhật 1 bản ghi đã tồn tại.
 * - delete: xóa 1 bản ghi.
 *
 * Các Dao cụ thể chỉ cần kế thừa BaseDao<T> để có sẵn các thao tác cơ bản,
 * và khai báo thêm các @Query riêng nếu cần.
 */
interface BaseDao<T> {
    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insert(entity: T)

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insertAll(entities: List<T>)

    @Update
    suspend fun update(entity: T)

    @Delete
    suspend fun delete(entity: T)
}