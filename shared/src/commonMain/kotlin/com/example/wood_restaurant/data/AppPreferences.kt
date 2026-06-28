package com.example.wood_restaurant.data

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

    fun clear() = settings.clear()

    private companion object {
        const val KEY_ONBOARDING = "onboarding_done"
        const val KEY_USERNAME = "user_name"
    }
}
