package com.example.wood_restaurant.data

import com.example.wood_restaurant.config.SecretKeys
import com.example.wood_restaurant.data.remote.NaverLocalApi
import com.example.wood_restaurant.data.remote.NaverReverseGeocodeApi
import com.example.wood_restaurant.data.remote.dto.NaverLocalItem
import com.example.wood_restaurant.domain.LatLng
import com.example.wood_restaurant.domain.PlaceCategory
import com.example.wood_restaurant.domain.Restaurant
import com.example.wood_restaurant.domain.distanceTo
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * 주변 장소 검색.
 *
 * 네이버 지역검색 API의 제약을 이 클래스에서 흡수한다.
 * 1. 좌표/반경 파라미터가 없다  → 리버스 지오코딩으로 얻은 지역명을 검색어에 붙인다.
 * 2. 한 질의당 최대 5건이다     → 카테고리별 키워드로 쪼개 병렬 질의 후 합친다.
 * 3. 평점/리뷰수가 없다         → [RatingSource]가 채운다.
 */
class PlaceRepository(
    private val localApi: NaverLocalApi,
    private val reverseGeocodeApi: NaverReverseGeocodeApi,
    private val ratingSource: RatingSource,
) {

    /**
     * [center] 주변에서 [categories]에 해당하는 장소를 모아 온다.
     * 반경 필터/정렬은 도메인 계층(applyFilter)에서 처리하므로 여기서는 거리만 계산해 붙인다.
     */
    suspend fun searchNearby(
        center: LatLng,
        categories: Set<PlaceCategory>,
        extraKeyword: String = "",
    ): NearbyResult = coroutineScope {
        require(SecretKeys.isOpenApiConfigured) {
            "네이버 검색 오픈API 키가 없습니다. local.properties에 naver.openapi.clientId/clientSecret을 설정하세요."
        }

        val regionName = resolveRegionName(center)

        val queries = categories.flatMap { category ->
            category.searchKeywords.map { keyword ->
                buildString {
                    if (regionName != null) append(regionName).append(' ')
                    if (extraKeyword.isNotBlank()) append(extraKeyword.trim()).append(' ')
                    append(keyword)
                }
            }
        }.distinct()

        val items = queries
            .map { query ->
                async {
                    runCatching {
                        localApi.searchLocal(query = query, display = NaverLocalApi.MAX_DISPLAY).items
                    }.getOrElse { emptyList() }
                }
            }
            .awaitAll()
            .flatten()

        val places = items
            .mapNotNull { it.toRestaurant(center) }
            .filter { it.category in categories }
            // 같은 가게가 여러 키워드 질의에 중복으로 잡힌다.
            .distinctBy { it.id }

        NearbyResult(
            places = places,
            regionName = regionName,
            queryCount = queries.size,
        )
    }

    /** 좌표 → "서울특별시 강남구 역삼동". 키가 없거나 실패하면 null(지역명 없이 검색). */
    private suspend fun resolveRegionName(center: LatLng): String? {
        if (!SecretKeys.isReverseGeocodeConfigured) return null
        return runCatching {
            reverseGeocodeApi
                .reverseGeocode(coords = "${center.longitude},${center.latitude}")
                .regionName()
        }.getOrNull()
    }

    private fun NaverLocalItem.toRestaurant(center: LatLng): Restaurant? {
        val position = parsePosition() ?: return null
        val placeCategory = PlaceCategory.fromNaverCategory(category) ?: return null
        val cleanName = title.stripHtml()
        if (cleanName.isBlank()) return null

        // 지역검색에는 안정적인 고유 id가 없다. 상호+도로명주소 조합을 키로 쓴다.
        val id = "$cleanName@${roadAddress.ifBlank { address }}"

        return Restaurant(
            id = id,
            name = cleanName,
            category = placeCategory,
            categoryDetail = category,
            roadAddress = roadAddress,
            address = address,
            telephone = telephone,
            link = link,
            position = position,
            rating = ratingSource.ratingOf(id),
            reviewCount = ratingSource.reviewCountOf(id),
            distanceMeters = center.distanceTo(position),
        )
    }
}

data class NearbyResult(
    val places: List<Restaurant>,
    /** 검색에 사용한 지역명. null이면 리버스 지오코딩 없이 검색한 것. */
    val regionName: String?,
    val queryCount: Int,
)

/** mapx/mapy는 WGS84 좌표를 10^7배한 정수 문자열이다. */
private fun NaverLocalItem.parsePosition(): LatLng? {
    val x = mapx.toDoubleOrNull() ?: return null
    val y = mapy.toDoubleOrNull() ?: return null
    if (x == 0.0 && y == 0.0) return null
    val lng = x / 1e7
    val lat = y / 1e7
    // 한반도 밖이면 좌표계가 예상과 다른 것이므로 버린다.
    if (lat !in 32.0..39.5 || lng !in 124.0..132.5) return null
    return LatLng(latitude = lat, longitude = lng)
}

/** 검색어 강조 태그(`<b>`)와 HTML 엔티티를 제거한다. */
private fun String.stripHtml(): String =
    replace(HTML_TAG_REGEX, "")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&apos;", "'")
        .replace("&nbsp;", " ")
        .trim()

private val HTML_TAG_REGEX = Regex("<[^>]*>")
