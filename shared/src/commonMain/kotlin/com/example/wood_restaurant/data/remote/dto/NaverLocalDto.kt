package com.example.wood_restaurant.data.remote.dto

import kotlinx.serialization.Serializable

/** 네이버 지역검색 API(`/v1/search/local.json`) 응답. */
@Serializable
data class NaverLocalResponse(
    val lastBuildDate: String = "",
    val total: Int = 0,
    val start: Int = 0,
    val display: Int = 0,
    val items: List<NaverLocalItem> = emptyList(),
)

/**
 * 지역검색 결과 한 건.
 *
 * - [title]에는 검색어 강조용 `<b>` 태그가 섞여 온다.
 * - [mapx]/[mapy]는 WGS84 경위도를 10^7 배한 정수 문자열이다. (예: "1270276250" → 127.0276250)
 * - 평점·리뷰수 필드는 이 API에 존재하지 않는다.
 */
@Serializable
data class NaverLocalItem(
    val title: String = "",
    val link: String = "",
    val category: String = "",
    val description: String = "",
    val telephone: String = "",
    val address: String = "",
    val roadAddress: String = "",
    val mapx: String = "",
    val mapy: String = "",
)
