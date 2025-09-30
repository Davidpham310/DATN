package com.example.datn.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.datn.data.local.dao.UserDao
import com.example.datn.data.local.entities.UserEntity

@Database(entities = [UserEntity::class ], version = 2)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
}