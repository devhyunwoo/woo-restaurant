package com.example.wood_restaurant.data

import com.example.wood_restaurant.domain.LatLng
import com.russhwolf.settings.Settings
import com.russhwolf.settings.get
import com.russhwolf.settings.set

/** 앱 설정/사용자 환경값을 타입 안전하게 다루는 래퍼. */
class AppPreferences(
    private val settings: Settings,
) {
    var isOnboardingDone: Boolean
        get() = settings[KEY_ONBOARDING, false]
        set(value) {
            settings[KEY_ONBOARDING] = value
        }

    var lastUserName: String?
        get() = settings[KEY_USERNAME]
        set(value) {
            if (value == null) settings.remove(KEY_USERNAME) else settings[KEY_USERNAME] = value
        }

    /** 최근 검색어. 최신이 앞. */
    var recentKeywords: List<String>
        get() = settings.getStringOrNull(KEY_RECENT_KEYWORDS)
            ?.split(LIST_SEPARATOR)
            ?.filter { it.isNotBlank() }
            ?: emptyList()
        set(value) {
            settings.putString(KEY_RECENT_KEYWORDS, value.joinToString(LIST_SEPARATOR))
        }

    /** 검색어를 맨 앞에 넣고 중복은 제거, 최대 [MAX_RECENT_KEYWORDS]개만 유지한다. */
    fun pushRecentKeyword(keyword: String) {
        val trimmed = keyword.trim()
        if (trimmed.isEmpty()) return
        recentKeywords = (listOf(trimmed) + recentKeywords.filter { it != trimmed })
            .take(MAX_RECENT_KEYWORDS)
    }

    fun clearRecentKeywords() {
        settings.remove(KEY_RECENT_KEYWORDS)
    }

    /** 마지막으로 검색한 위치. 재실행 시 위치 권한이 없어도 이 근처부터 보여준다. */
    var lastSearchCenter: LatLng?
        get() {
            val lat = settings.getDoubleOrNull(KEY_LAST_LAT) ?: return null
            val lng = settings.getDoubleOrNull(KEY_LAST_LNG) ?: return null
            return LatLng(latitude = lat, longitude = lng)
        }
        set(value) {
            if (value == null) {
                settings.remove(KEY_LAST_LAT)
                settings.remove(KEY_LAST_LNG)
            } else {
                settings.putDouble(KEY_LAST_LAT, value.latitude)
                settings.putDouble(KEY_LAST_LNG, value.longitude)
            }
        }

    /** 지도를 멈추면 자동으로 그 지역을 재검색할지. */
    var isAutoResearchEnabled: Boolean
        get() = settings[KEY_AUTO_RESEARCH, false]
        set(value) {
            settings[KEY_AUTO_RESEARCH] = value
        }

    fun clear() = settings.clear()

    private companion object {
        const val KEY_ONBOARDING = "onboarding_done"
        const val KEY_USERNAME = "user_name"
        const val KEY_RECENT_KEYWORDS = "recent_keywords"
        const val KEY_LAST_LAT = "last_search_lat"
        const val KEY_LAST_LNG = "last_search_lng"
        const val KEY_AUTO_RESEARCH = "auto_research"

        const val MAX_RECENT_KEYWORDS = 8

        /** 검색어에 절대 안 들어갈 제어문자(Unit Separator)로 리스트를 잇는다. */
        const val LIST_SEPARATOR = "\u001F"
    }
}
