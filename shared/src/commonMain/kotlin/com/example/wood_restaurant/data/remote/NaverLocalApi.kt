package com.example.wood_restaurant.data.remote

import com.example.wood_restaurant.data.remote.dto.NaverLocalResponse
import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.Query

/**
 * 네이버 검색 오픈API — 지역(장소) 검색.
 *
 * 주의: [display]는 최대 5, [start]는 1만 허용된다. 즉 한 질의로 최대 5건이다.
 * 그래서 [com.example.wood_restaurant.data.PlaceRepository]에서 키워드를 쪼개 여러 번 질의한다.
 */
interface NaverLocalApi {
    @GET("v1/search/local.json")
    suspend fun searchLocal(
        @Query("query") query: String,
        @Query("display") display: Int = MAX_DISPLAY,
        @Query("start") start: Int = 1,
        /** "random"(정확도순) 또는 "comment"(리뷰 많은 순). */
        @Query("sort") sort: String = "random",
    ): NaverLocalResponse

    companion object {
        const val MAX_DISPLAY = 5
    }
}
