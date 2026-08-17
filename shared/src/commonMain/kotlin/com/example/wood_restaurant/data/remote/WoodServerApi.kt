package com.example.wood_restaurant.data.remote

import com.example.wood_restaurant.domain.Restaurant
import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.Query
import kotlinx.serialization.Serializable

/**
 * 우리 백엔드(woodrestaurant-server) 응답. 서버의 `NearbyPlacesResponse`와 필드가 1:1이라
 * `places`를 도메인 [Restaurant]로 바로 받는다 — 서버 쪽 `PlaceControllerTest`가 이 계약을 고정한다.
 */
@Serializable
data class NearbyPlacesResponse(
    val places: List<Restaurant> = emptyList(),
    val regionName: String? = null,
    val queryCount: Int = 0,
)

/**
 * 백엔드 API. 네이버 시크릿은 서버가 들고 있으므로 여기엔 인증 헤더가 없다.
 * (지금은 공개 API. 로그인이 생기면 토큰 헤더가 붙는다.)
 */
interface WoodServerApi {
    @GET("api/v1/places")
    suspend fun nearbyPlaces(
        @Query("lat") latitude: Double,
        @Query("lng") longitude: Double,
        /** "RESTAURANT,CAFE" — 서버는 대소문자 무관, 비면 전체. */
        @Query("categories") categories: String,
        @Query("keyword") keyword: String,
    ): NearbyPlacesResponse
}
