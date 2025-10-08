package com.example.datn.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.datn.data.local.entities.UserEntity

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: UserEntity)

    @Update
    suspend fun update(user: UserEntity)

    @Delete
    suspend fun delete(user: UserEntity)


    @Query("SELECT * FROM user WHERE id = :userId")
    suspend fun getUserById(userId: String): UserEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM user WHERE id = :id)")
    suspend fun isUserExists(id: String): Boolean

    @Query("SELECT * FROM user")
    suspend fun getAllUsers(): List<UserEntity>
}