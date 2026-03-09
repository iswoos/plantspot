package com.studio.plantspot.data.repository

import com.studio.plantspot.domain.entity.Memo
import com.studio.plantspot.domain.repository.MemoRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.OffsetDateTime
import java.time.ZoneOffset
import javax.inject.Inject

@Serializable
private data class MemoDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    val title: String,
    val content: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
private data class MemoInsertDto(
    @SerialName("user_id") val userId: String,
    val title: String,
    val content: String
)

@Serializable
private data class MemoUpdateDto(
    val title: String,
    val content: String,
    @SerialName("updated_at") val updatedAt: String
)

class MemoRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient
) : MemoRepository {

    private val refreshSignal = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    private suspend fun triggerRefresh() {
        refreshSignal.emit(Unit)
    }

    override fun getMemos(): Flow<List<Memo>> = refreshSignal
        .onStart { emit(Unit) }
        .flatMapLatest {
            flow {
                val userId = supabase.auth.currentUserOrNull()?.id ?: throw Exception("로그인이 필요합니다.")
                val response = supabase.postgrest.from("plantspot_user_memos")
                    .select {
                        filter { eq("user_id", userId) }
                        order("updated_at", Order.DESCENDING)
                    }
                val dtoList = response.decodeList<MemoDto>()
                emit(dtoList.map { it.toDomain() })
            }
        }

    override suspend fun createMemo(title: String, content: String) {
        val userId = supabase.auth.currentUserOrNull()?.id ?: throw Exception("로그인이 필요합니다.")
        supabase.postgrest.from("plantspot_user_memos").insert(
            MemoInsertDto(userId = userId, title = title, content = content)
        )
        triggerRefresh()
    }

    override suspend fun updateMemo(id: String, title: String, content: String) {
        val now = OffsetDateTime.now(ZoneOffset.UTC).toString()
        supabase.postgrest.from("plantspot_user_memos").update(
            MemoUpdateDto(title = title, content = content, updatedAt = now)
        ) {
            filter { eq("id", id) }
        }
        triggerRefresh()
    }

    override suspend fun deleteMemo(id: String) {
        supabase.postgrest.from("plantspot_user_memos").delete {
            filter { eq("id", id) }
        }
        triggerRefresh()
    }

    private fun MemoDto.toDomain(): Memo = Memo(
        id = id,
        userId = userId,
        title = title,
        content = content,
        createdAt = OffsetDateTime.parse(createdAt),
        updatedAt = OffsetDateTime.parse(updatedAt)
    )
}
