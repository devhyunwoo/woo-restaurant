package com.example.wood_restaurant.domain

/**
 * 홈 화면에서 노출하는 업종 카테고리.
 *
 * [searchKeywords]는 네이버 지역검색 API 질의에 사용한다.
 * 지역검색은 한 번에 최대 5건만 돌려주므로, 카테고리마다 여러 키워드로 나눠 질의한 뒤 합친다.
 */
enum class PlaceCategory(
    val label: String,
    val emoji: String,
    val searchKeywords: List<String>,
) {
    RESTAURANT("식당", "🍚", listOf("맛집", "음식점", "한식", "일식")),
    CAFE("커피", "☕", listOf("카페", "커피", "디저트")),
    BAKERY("빵집", "🥐", listOf("베이커리", "빵집", "제과점")),
    ;

    companion object {
        /**
         * 네이버 지역검색의 category 문자열을 카테고리로 매핑한다.
         * 예) "음식점>카페,디저트>베이커리" → [BAKERY], "음식점>한식>냉면" → [RESTAURANT]
         *
         * 베이커리는 네이버 분류상 카페 하위에 있으므로 반드시 카페보다 먼저 검사한다.
         */
        fun fromNaverCategory(raw: String): PlaceCategory? = when {
            raw.contains("베이커리") || raw.contains("제과") || raw.contains("빵") -> BAKERY
            raw.contains("카페") || raw.contains("커피") || raw.contains("디저트") -> CAFE
            raw.contains("음식점") || raw.contains("식당") -> RESTAURANT
            else -> null
        }
    }
}
