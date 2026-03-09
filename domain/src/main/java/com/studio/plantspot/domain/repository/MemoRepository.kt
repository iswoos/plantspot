package com.studio.plantspot.domain.repository

import com.studio.plantspot.domain.entity.Memo
import kotlinx.coroutines.flow.Flow

interface MemoRepository {
    fun getMemos(): Flow<List<Memo>>
    suspend fun createMemo(title: String, content: String)
    suspend fun updateMemo(id: String, title: String, content: String)
    suspend fun deleteMemo(id: String)
}
