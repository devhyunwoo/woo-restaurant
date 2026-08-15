package com.example.wood_restaurant.domain

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlaceFilterTest {

    private fun place(
        id: String,
        category: PlaceCategory = PlaceCategory.RESTAURANT,
        distance: Double = 100.0,
        rating: Double? = 4.0,
        reviewCount: Int? = 100,
        name: String = id,
        categoryDetail: String = "음식점>한식",
    ) = Restaurant(
        id = id,
        name = name,
        category = category,
        categoryDetail = categoryDetail,
        roadAddress = "서울 어딘가 1길 2",
        address = "서울 어딘가동 3-4",
        telephone = "",
        link = "",
        position = LatLng(37.5, 127.0),
        rating = rating,
        reviewCount = reviewCount,
        distanceMeters = distance,
    )

    private val sample = listOf(
        place("a", PlaceCategory.RESTAURANT, distance = 800.0, rating = 4.8, reviewCount = 20),
        place("b", PlaceCategory.CAFE, distance = 150.0, rating = 3.2, reviewCount = 900),
        place("c", PlaceCategory.BAKERY, distance = 2500.0, rating = 4.9, reviewCount = 500),
        place("d", PlaceCategory.RESTAURANT, distance = 400.0, rating = null, reviewCount = null),
    )

    @Test
    fun `기본 필터는 1km 안의 장소만 가까운 순으로 준다`() {
        val result = sample.applyFilter(PlaceFilter())

        assertEquals(listOf("b", "d", "a"), result.map { it.id })
    }

    @Test
    fun `카테고리 필터와 정렬은 함께 걸린다`() {
        val result = sample.applyFilter(
            PlaceFilter(
                categories = setOf(PlaceCategory.RESTAURANT, PlaceCategory.BAKERY),
                sort = SortOption.RATING,
                radius = SearchRadius.R_3000,
            )
        )

        // 카페(b)는 빠지고, 별점 내림차순. 별점 없는 d는 맨 뒤.
        assertEquals(listOf("c", "a", "d"), result.map { it.id })
    }

    @Test
    fun `리뷰순 정렬에서 리뷰수 없는 항목은 뒤로 간다`() {
        val result = sample.applyFilter(
            PlaceFilter(sort = SortOption.REVIEW, radius = SearchRadius.R_5000)
        )

        assertEquals(listOf("b", "c", "a", "d"), result.map { it.id })
    }

    @Test
    fun `최소 별점 필터는 별점 없는 항목을 제외한다`() {
        val result = sample.applyFilter(
            PlaceFilter(radius = SearchRadius.R_5000, minRating = MinRating.R_4_5)
        )

        assertEquals(setOf("a", "c"), result.map { it.id }.toSet())
    }

    @Test
    fun `키워드는 이름과 업종과 주소를 함께 본다`() {
        val places = listOf(
            place("x", name = "우드 파스타", categoryDetail = "음식점>양식>파스타"),
            place("y", name = "김밥천국", categoryDetail = "음식점>분식"),
        )

        assertEquals(
            listOf("x"),
            places.applyFilter(PlaceFilter(keyword = "파스타")).map { it.id },
        )
    }

    @Test
    fun `activeCount는 기본값에서 벗어난 축의 수를 센다`() {
        assertEquals(0, PlaceFilter().activeCount)
        assertEquals(
            2,
            PlaceFilter(sort = SortOption.RATING, radius = SearchRadius.R_3000).activeCount,
        )
    }

    @Test
    fun `카테고리 매핑은 베이커리를 카페보다 먼저 판정한다`() {
        assertEquals(
            PlaceCategory.BAKERY,
            PlaceCategory.fromNaverCategory("음식점>카페,디저트>베이커리"),
        )
        assertEquals(
            PlaceCategory.CAFE,
            PlaceCategory.fromNaverCategory("음식점>카페,디저트>커피전문점"),
        )
        assertEquals(
            PlaceCategory.RESTAURANT,
            PlaceCategory.fromNaverCategory("음식점>한식>냉면"),
        )
        assertEquals(null, PlaceCategory.fromNaverCategory("생활,편의>세탁"))
    }

    @Test
    fun `거리 계산은 서울시청 기준 광화문까지 약 1km 안팎이다`() {
        val cityHall = LatLng(37.5666102, 126.9783881)
        val gwanghwamun = LatLng(37.5759, 126.9769)

        val meters = cityHall.distanceTo(gwanghwamun)

        assertTrue(abs(meters - 1_050) < 150, "예상 밖 거리: $meters")
    }
}
