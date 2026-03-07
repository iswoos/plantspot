package com.studio.plantspot.domain.entity

data class UserProfile(
    val id: String,
    val email: String?,
    val displayName: String?,
    val photoUrl: String?,
    val provider: String,
    val locale: String?,
    val nickname: String? = null
)
