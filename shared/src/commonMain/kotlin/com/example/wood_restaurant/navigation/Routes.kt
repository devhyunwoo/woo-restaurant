package com.example.wood_restaurant.navigation

import kotlinx.serialization.Serializable

/** 타입 세이프 네비게이션 라우트. 인자는 생성자 파라미터로 표현한다. */
@Serializable
data object HomeRoute

@Serializable
data class DetailRoute(val postId: Int)
