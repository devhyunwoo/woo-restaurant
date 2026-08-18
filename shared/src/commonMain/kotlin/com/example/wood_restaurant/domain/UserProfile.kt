package com.example.wood_restaurant.domain

import kotlinx.serialization.Serializable

/** 로그인한 사용자. 서버 `UserResponse`와 같은 필드 — 세션 저장용으로도 직렬화한다. */
@Serializable
data class UserProfile(
    val id: Long,
    val email: String,
    val nickname: String,
)
