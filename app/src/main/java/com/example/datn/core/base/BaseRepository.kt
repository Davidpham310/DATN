package com.example.datn.core.base

import com.example.datn.core.utils.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow

abstract class BaseRepository {
    protected fun <T> handleFlow(
        localAction: suspend () -> T?,
        remoteAction: suspend () -> T,
        saveLocal: suspend (T) -> Unit
    ): Flow<Resource<T>> =
        flow {
            emit(Resource.Loading())
            // Emit local data first
            localAction()?.let { emit(Resource.Success(it)) }
            // Then fetch remote
            val remoteData = remoteAction()
            saveLocal(remoteData)
            emit(Resource.Success(remoteData))
        }.catch { e ->
            // Bắt lỗi ở đây thay vì emit trong try/catch
            emit(Resource.Error(e.message ?: "Error fetching data"))
        }

}