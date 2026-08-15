package com.example.wood_restaurant.data

import com.example.wood_restaurant.domain.Restaurant
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json

/**
 * 찜(즐겨찾기) 저장소.
 *
 * 장소 전체를 JSON으로 저장한다. 지역검색 API에는 안정적인 장소 id가 없어서
 * id만 저장해 두면 나중에 다시 조회할 방법이 없기 때문이다.
 * 대신 저장된 거리값은 저장 당시 기준이므로, 보여줄 때 현재 검색 중심으로 다시 계산해야 한다.
 */
class FavoritesRepository(
    private val settings: Settings,
) {
    private val json = Json { ignoreUnknownKeys = true }

    private val _favorites = MutableStateFlow(load())

    /** 최근에 찜한 것이 앞. */
    val favorites: StateFlow<List<Restaurant>> = _favorites.asStateFlow()

    fun isFavorite(placeId: String): Boolean = _favorites.value.any { it.id == placeId }

    /** 토글 후의 상태를 돌려준다. true면 방금 찜에 추가된 것. */
    fun toggle(place: Restaurant): Boolean {
        val current = _favorites.value
        val next = if (current.any { it.id == place.id }) {
            current.filterNot { it.id == place.id }
        } else {
            listOf(place) + current
        }
        _favorites.value = next
        save(next)
        return next.any { it.id == place.id }
    }

    fun clear() {
        _favorites.value = emptyList()
        settings.remove(KEY)
    }

    private fun load(): List<Restaurant> {
        val raw = settings.getStringOrNull(KEY) ?: return emptyList()
        // 스키마가 바뀌어 못 읽게 되면 찜을 잃는 게 앱이 안 켜지는 것보다 낫다.
        return runCatching { json.decodeFromString<List<Restaurant>>(raw) }.getOrDefault(emptyList())
    }

    private fun save(list: List<Restaurant>) {
        settings.putString(KEY, json.encodeToString(list))
    }

    private companion object {
        const val KEY = "favorite_places_v1"
    }
}
