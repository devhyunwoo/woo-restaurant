package com.example.wood_restaurant.navigation

import kotlinx.serialization.Serializable

/** 타입 세이프 네비게이션 라우트. 인자는 생성자 파라미터로 표현한다. */
@Serializable
data object MainRoute

/** 로그인/회원가입. 첫 진입(세션 없음)과 마이 탭의 "로그인" 버튼 두 곳에서 온다. */
@Serializable
data object LoginRoute
