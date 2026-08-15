package com.example.wood_restaurant.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlaceLinksTest {

    private val place = Restaurant(
        id = "우드 파스타@서울 마포구 어울마당로 1",
        name = "우드 파스타",
        category = PlaceCategory.RESTAURANT,
        categoryDetail = "음식점>양식>파스타",
        roadAddress = "서울 마포구 어울마당로 1",
        address = "서울 마포구 서교동 1-1",
        telephone = "02-123-4567",
        link = "",
        position = LatLng(37.5563, 126.9236),
        rating = 4.5,
        reviewCount = 120,
        distanceMeters = 340.0,
    )

    @Test
    fun `퍼센트 인코딩은 한글을 UTF-8 바이트 단위로 바꾸고 예약문자는 남긴다`() {
        assertEquals("%EC%9A%B0%EB%93%9C", percentEncode("우드"))
        assertEquals("a-b_c.d~e", percentEncode("a-b_c.d~e"))
        assertEquals("a%20b%26c%3Dd", percentEncode("a b&c=d"))
    }

    @Test
    fun `네이버 지도 길찾기 URL은 좌표와 인코딩된 이름과 appname을 담는다`() {
        val url = PlaceLinks.naverMapDirections(place, appIdentifier = "com.example.app")

        assertTrue(url.startsWith("nmap://route/public?"), url)
        assertTrue("dlat=37.5563" in url, url)
        assertTrue("dlng=126.9236" in url, url)
        assertTrue("dname=%EC%9A%B0%EB%93%9C%20%ED%8C%8C%EC%8A%A4%ED%83%80" in url, url)
        assertTrue(url.endsWith("&appname=com.example.app"), url)
    }

    @Test
    fun `장소 링크가 비어 있으면 웹 검색 URL로 대신한다`() {
        val url = PlaceLinks.placePageUrl(place)

        assertTrue(url.startsWith("https://map.naver.com/p/search/"), url)
        assertTrue(url.contains("%EC%9A%B0%EB%93%9C"), url)
    }

    @Test
    fun `공유 문구는 이름 · 업종 · 주소 · 전화 · 링크 순이다`() {
        val lines = PlaceLinks.shareText(place).lines()

        assertEquals("우드 파스타", lines[0])
        assertEquals("파스타", lines[1])
        assertEquals("서울 마포구 어울마당로 1", lines[2])
        assertEquals("02-123-4567", lines[3])
        assertTrue(lines[4].startsWith("https://"), lines[4])
    }

    @Test
    fun `도보 시간은 67m 분 기준이고 최소 1분이다`() {
        assertEquals(1, walkingMinutes(10.0))
        assertEquals(5, walkingMinutes(340.0))
        assertEquals(15, walkingMinutes(1000.0))
    }
}
