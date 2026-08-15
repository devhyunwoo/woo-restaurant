package com.example.wood_restaurant.data

/**
 * 별점·리뷰수 공급자.
 *
 * 네이버 지역검색 API는 이 두 값을 **제공하지 않는다**. 나중에 자체 백엔드가 붙으면
 * 이 인터페이스만 구현체를 갈아끼우면 되도록 정렬/필터 로직에서 분리해 두었다.
 */
interface RatingSource {
    fun ratingOf(placeId: String): Double?
    fun reviewCountOf(placeId: String): Int?
}

/** 실제 데이터가 없음을 그대로 드러내는 구현. UI에는 "–"로 표시된다. */
object EmptyRatingSource : RatingSource {
    override fun ratingOf(placeId: String): Double? = null
    override fun reviewCountOf(placeId: String): Int? = null
}

/**
 * 개발 중 별점순/리뷰순 정렬을 눈으로 확인하기 위한 스텁.
 * 장소 id에서 결정론적으로 값을 만들어내므로 새로고침해도 값이 흔들리지 않는다.
 * 실서비스에서는 반드시 실제 소스로 교체할 것.
 */
object StubRatingSource : RatingSource {
    override fun ratingOf(placeId: String): Double {
        // 3.0 ~ 4.9
        return (30 + placeId.stableHash() % 20) / 10.0
    }

    override fun reviewCountOf(placeId: String): Int {
        // 12 ~ 911
        return 12 + (placeId.stableHash() / 7) % 900
    }
}

private fun String.stableHash(): Int {
    var hash = 7
    for (char in this) {
        hash = (hash * 31 + char.code) and 0x7FFFFFFF
    }
    return hash
}
