package com.studio.plantspot.data.remote

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.realtime.Realtime

/**
 * Supabase 클라이언트 싱글턴
 * 접속에 필요한 URL과 ANON KEY는 보안 정책에 따라 상수로 관리됩니다.
 */
object SupabaseClient {
    private const val SUPABASE_URL = "YOUR_SUPABASE_URL"
    private const val SUPABASE_ANON_KEY = "YOUR_SUPABASE_ANON_KEY"

    val client = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_ANON_KEY
    ) {
        install(Postgrest)
        install(Auth)
        install(Realtime)
    }
}
