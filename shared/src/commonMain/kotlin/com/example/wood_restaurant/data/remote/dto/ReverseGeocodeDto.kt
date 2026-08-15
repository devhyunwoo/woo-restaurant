package com.example.wood_restaurant.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ReverseGeocodeResponse(
    val status: ReverseGeocodeStatus = ReverseGeocodeStatus(),
    val results: List<ReverseGeocodeResult> = emptyList(),
) {
    /** "서울특별시 강남구 역삼동" 형태의 지역명. 없으면 null. */
    fun regionName(): String? = results.firstNotNullOfOrNull { result ->
        val region = result.region ?: return@firstNotNullOfOrNull null
        listOfNotNull(
            region.area1?.name?.takeIf { it.isNotBlank() },
            region.area2?.name?.takeIf { it.isNotBlank() },
            region.area3?.name?.takeIf { it.isNotBlank() },
        ).takeIf { it.isNotEmpty() }?.joinToString(" ")
    }
}

@Serializable
data class ReverseGeocodeStatus(
    val code: Int = -1,
    val name: String = "",
    val message: String = "",
)

@Serializable
data class ReverseGeocodeResult(
    val name: String = "",
    val region: ReverseGeocodeRegion? = null,
)

@Serializable
data class ReverseGeocodeRegion(
    val area1: ReverseGeocodeArea? = null,
    val area2: ReverseGeocodeArea? = null,
    val area3: ReverseGeocodeArea? = null,
    val area4: ReverseGeocodeArea? = null,
)

@Serializable
data class ReverseGeocodeArea(
    val name: String = "",
)
