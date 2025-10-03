package com.example.datn.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.datn.data.local.entities.UserEntity

@Dao
interface UserDao {
    @Insert
    suspend fun insertUser(user: UserEntity)

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getUser(id: String): UserEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM users WHERE id = :id)")
    suspend fun isUserExists(id: String): Boolean

    @Query("SELECT * FROM users")
    suspend fun getAllUsers(): List<UserEntity>
}