package com.example.wood_restaurant.domain

import kotlinx.serialization.Serializable

/**
 * 홈 지도/목록에 그려지는 장소 한 건.
 *
 * [rating]/[reviewCount]는 nullable이다. 네이버 지역검색 API가 평점·리뷰수를 주지 않기 때문이며,
 * 값을 채우는 책임은 [com.example.wood_restaurant.data.RatingSource]에 있다.
 *
 * 찜 목록에 그대로 저장되므로 직렬화 가능해야 한다.
 */
@Serializable
data class Restaurant(
    val id: String,
    val name: String,
    val category: PlaceCategory,
    /** 네이버 원본 분류 문자열. 예) "음식점>한식>냉면" */
    val categoryDetail: String,
    val roadAddress: String,
    val address: String,
    val telephone: String,
    val link: String,
    val position: LatLng,
    val rating: Double?,
    val reviewCount: Int?,
    /** 검색 기준점으로부터의 거리(미터). */
    val distanceMeters: Double,
) {
    /** "음식점>한식>냉면" → "냉면" */
    val subCategory: String
        get() = categoryDetail.substringAfterLast('>').trim()

    val distanceLabel: String
        get() = formatDistance(distanceMeters)

    /** "도보 3분" */
    val walkingLabel: String
        get() = "도보 ${walkingMinutes(distanceMeters)}분"

    /** 도로명 주소가 있으면 그것, 없으면 지번. */
    val displayAddress: String
        get() = roadAddress.ifBlank { address }
}
