package com.example.wood_restaurant.data

import com.example.wood_restaurant.data.remote.WoodServerApi
import com.example.wood_restaurant.domain.LatLng
import com.example.wood_restaurant.domain.PlaceCategory

/**
 * 우리 백엔드를 통해 검색한다. 네이버 시크릿이 앱에 없어도 되고, 캐시도 서버에서 모든 사용자가 나눠 쓴다.
 *
 * 서버가 아직 평점을 주지 않으므로(항상 null) [RatingSource]로 앱에서 채운다.
 * 서버에 리뷰 기능이 생기면 이 보정을 빼면 된다.
 */
class RemotePlaceRepository(
    private val api: WoodServerApi,
    private val ratingSource: RatingSource,
) : PlaceRepository {

    override suspend fun searchNearby(
        center: LatLng,
        categories: Set<PlaceCategory>,
        extraKeyword: String,
    ): NearbyResult {
        val response = api.nearbyPlaces(
            latitude = center.latitude,
            longitude = center.longitude,
            categories = categories.joinToString(",") { it.name },
            keyword = extraKeyword.trim(),
        )
        return NearbyResult(
            places = response.places.map { place ->
                if (place.rating == null && place.reviewCount == null) {
                    place.copy(
                        rating = ratingSource.ratingOf(place.id),
                        reviewCount = ratingSource.reviewCountOf(place.id),
                    )
                } else {
                    place
                }
            },
            regionName = response.regionName,
            queryCount = response.queryCount,
        )
    }
}
