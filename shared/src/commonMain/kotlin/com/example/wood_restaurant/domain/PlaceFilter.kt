package com.example.wood_restaurant.domain

/** 정렬 기준. */
enum class SortOption(val label: String) {
    DISTANCE("가까운순"),
    RATING("별점순"),
    REVIEW("리뷰순"),
    NAME("이름순"),
}

/** 검색 반경. 지역검색 API에는 반경 파라미터가 없어서 좌표 거리로 직접 걸러낸다. */
enum class SearchRadius(val meters: Int, val label: String) {
    R_500(500, "500m"),
    R_1000(1000, "1km"),
    R_3000(3000, "3km"),
    R_5000(5000, "5km"),
}

/** 최소 별점 필터. */
enum class MinRating(val value: Double, val label: String) {
    ANY(0.0, "전체"),
    R_3_5(3.5, "3.5+"),
    R_4_0(4.0, "4.0+"),
    R_4_5(4.5, "4.5+"),
}

/**
 * 홈 화면에서 동시에 걸 수 있는 필터 축.
 * 카테고리(다중) · 정렬 · 반경 · 최소 별점 · 키워드 · 찜만 — 모두 AND로 조합된다.
 */
data class PlaceFilter(
    val categories: Set<PlaceCategory> = PlaceCategory.entries.toSet(),
    val sort: SortOption = SortOption.DISTANCE,
    val radius: SearchRadius = SearchRadius.R_1000,
    val minRating: MinRating = MinRating.ANY,
    val keyword: String = "",
    /** 켜면 검색 결과 대신 찜 목록을 보여준다. 이때 반경은 무시한다(멀어도 내 찜은 보고 싶으니까). */
    val favoritesOnly: Boolean = false,
) {
    /** 기본값과 다른 축이 몇 개인지 — 필터 버튼 배지에 쓴다. */
    val activeCount: Int
        get() = listOf(
            categories.size != PlaceCategory.entries.size,
            sort != SortOption.DISTANCE,
            radius != SearchRadius.R_1000,
            minRating != MinRating.ANY,
            keyword.isNotBlank(),
            favoritesOnly,
        ).count { it }
}

/**
 * 필터 적용 + 정렬. 순수 함수라 단위 테스트로 검증한다.
 *
 * @param applyRadius false면 반경 조건을 건너뛴다(찜만 보기).
 */
fun List<Restaurant>.applyFilter(
    filter: PlaceFilter,
    applyRadius: Boolean = true,
): List<Restaurant> =
    asSequence()
        .filter { it.category in filter.categories }
        .filter { !applyRadius || it.distanceMeters <= filter.radius.meters }
        .filter { filter.minRating == MinRating.ANY || (it.rating ?: 0.0) >= filter.minRating.value }
        .filter { place ->
            filter.keyword.isBlank() ||
                place.name.contains(filter.keyword, ignoreCase = true) ||
                place.categoryDetail.contains(filter.keyword, ignoreCase = true) ||
                place.roadAddress.contains(filter.keyword, ignoreCase = true)
        }
        .sortedWith(filter.sort.comparator())
        .toList()

private fun SortOption.comparator(): Comparator<Restaurant> = when (this) {
    // 값이 없는 항목(rating/reviewCount == null)은 항상 뒤로 민다.
    SortOption.DISTANCE -> compareBy { it.distanceMeters }
    SortOption.RATING -> compareByDescending<Restaurant> { it.rating ?: -1.0 }
        .thenBy { it.distanceMeters }
    SortOption.REVIEW -> compareByDescending<Restaurant> { it.reviewCount ?: -1 }
        .thenBy { it.distanceMeters }
    SortOption.NAME -> compareBy<Restaurant> { it.name }.thenBy { it.distanceMeters }
}
