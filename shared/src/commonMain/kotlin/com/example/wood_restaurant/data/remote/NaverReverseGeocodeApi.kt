package com.example.wood_restaurant.data.remote

import com.example.wood_restaurant.data.remote.dto.ReverseGeocodeResponse
import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.Query

/**
 * 네이버 클라우드 플랫폼 Maps — 리버스 지오코딩.
 *
 * 지역검색 API에는 좌표/반경 파라미터가 없어서 "무엇을" 검색할지에 지역명이 필요하다.
 * 현재 좌표 → "서울특별시 강남구 역삼동"을 얻어 검색어 앞에 붙이는 용도로만 쓴다.
 */
interface NaverReverseGeocodeApi {
    @GET("map-reversegeocode/v2/gc")
    suspend fun reverseGeocode(
        /** "경도,위도" 순서. 위경도 순서가 아니라는 점에 주의. */
        @Query("coords") coords: String,
        @Query("orders") orders: String = "admcode,legalcode",
        @Query("output") output: String = "json",
    ): ReverseGeocodeResponse
}
