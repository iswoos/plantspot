package com.studio.plantspot.data.local

import android.content.Context
import android.content.SharedPreferences
import java.util.UUID

/**
 * 로컬 사용자 ID 관리 (OAuth 미구현 전 임시)
 * SharedPreferences에 UUID를 저장하고, 앱 최초 실행 시 자동 생성합니다.
 * 이후 실제 인증 연동 시 getUserId() 반환값만 교체하면 됩니다.
 */
object UserPreferences {

    private const val PREFS_NAME = "plantspot_prefs"
    private const val KEY_USER_ID = "local_user_id"

    private lateinit var prefs: SharedPreferences

    /**
     * Application.onCreate()에서 한 번 초기화 필요
     */
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * 로컬 UUID 반환 (없으면 신규 생성 후 저장)
     */
    fun getUserId(): String {
        val existing = prefs.getString(KEY_USER_ID, null)
        if (existing != null) return existing

        val newId = UUID.randomUUID().toString()
        prefs.edit().putString(KEY_USER_ID, newId).apply()
        return newId
    }
}
