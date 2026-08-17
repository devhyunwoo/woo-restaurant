package com.example.wood_restaurant.data.remote

import com.example.wood_restaurant.domain.PlaceCategory
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * 백엔드(woodrestaurant-server) 응답 계약.
 * 아래 JSON은 서버 쪽 PlaceControllerTest 가 고정한 형식 그대로다 — 양쪽 테스트가 같은 문자열을 보고 있어야
 * 한쪽이 필드를 바꿨을 때 다른 쪽에서 바로 깨진다.
 */
class WoodServerContractTest {

    private val json = Json { ignoreUnknownKeys = true }

    private val serverResponse = """
        {
          "places": [{
            "id": "우드 파스타@서울 강남구 강남대로 1",
            "name": "우드 파스타",
            "category": "RESTAURANT",
            "categoryDetail": "음식점>양식>파스타",
            "roadAddress": "서울 강남구 강남대로 1",
            "address": "서울 강남구 역삼동 1-1",
            "telephone": "02-000-0000",
            "link": "https://naver.me/x",
            "position": { "latitude": 37.4979, "longitude": 127.0276 },
            "rating": null,
            "reviewCount": null,
            "distanceMeters": 123.4
          }],
          "regionName": "서울특별시 강남구 역삼동",
          "queryCount": 4
        }
    """.trimIndent()

    @Test
    fun `서버 응답이 도메인 Restaurant 로 그대로 디코딩된다`() {
        val decoded = json.decodeFromString<NearbyPlacesResponse>(serverResponse)

        assertEquals("서울특별시 강남구 역삼동", decoded.regionName)
        assertEquals(4, decoded.queryCount)
        val place = decoded.places.single()
        assertEquals("우드 파스타@서울 강남구 강남대로 1", place.id)
        assertEquals("우드 파스타", place.name)
        assertEquals(PlaceCategory.RESTAURANT, place.category)
        assertEquals("파스타", place.subCategory)
        assertEquals(37.4979, place.position.latitude)
        assertEquals(127.0276, place.position.longitude)
        assertEquals(123.4, place.distanceMeters)
        assertNull(place.rating)
        assertNull(place.reviewCount)
    }

    @Test
    fun `서버가 필드를 추가해도 앱은 깨지지 않는다`() {
        val withExtra = serverResponse.replace(""""queryCount": 4""", """"queryCount": 4, "serverVersion": "1.2.0"""")
        val decoded = json.decodeFromString<NearbyPlacesResponse>(withExtra)
        assertEquals(1, decoded.places.size)
    }
}
