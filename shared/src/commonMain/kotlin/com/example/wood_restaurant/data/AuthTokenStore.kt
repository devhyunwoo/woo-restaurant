package com.example.wood_restaurant.data

import com.example.wood_restaurant.domain.UserProfile
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json

/** 로그인 세션 = 토큰 쌍 + 사용자. 앱을 다시 켜도 유지된다. */
data class AuthSession(
    val accessToken: String,
    val refreshToken: String,
    val user: UserProfile,
)

/**
 * 세션 저장소. 진실의 원천은 여기 하나고, 화면은 [session] 을 구독한다.
 *
 * TODO: 지금은 다른 설정과 같은 Settings(SharedPreferences/NSUserDefaults)에 저장한다.
 *  출시 전에 EncryptedSharedPreferences / Keychain 백엔드로 바꾼다 — 인터페이스는 그대로.
 */
class AuthTokenStore(
    private val settings: Settings,
) {
    private val json = Json { ignoreUnknownKeys = true }

    private val _session = MutableStateFlow(load())
    val session: StateFlow<AuthSession?> = _session.asStateFlow()

    val current: AuthSession? get() = _session.value

    fun save(session: AuthSession) {
        settings.putString(KEY_ACCESS, session.accessToken)
        settings.putString(KEY_REFRESH, session.refreshToken)
        settings.putString(KEY_USER, json.encodeToString(session.user))
        _session.value = session
    }

    fun clear() {
        settings.remove(KEY_ACCESS)
        settings.remove(KEY_REFRESH)
        settings.remove(KEY_USER)
        _session.value = null
    }

    private fun load(): AuthSession? {
        val access = settings.getStringOrNull(KEY_ACCESS) ?: return null
        val refresh = settings.getStringOrNull(KEY_REFRESH) ?: return null
        val userRaw = settings.getStringOrNull(KEY_USER) ?: return null
        // 사용자 스키마가 바뀌어 못 읽으면 로그아웃 상태로 시작하는 게 앱이 죽는 것보다 낫다.
        val user = runCatching { json.decodeFromString<UserProfile>(userRaw) }.getOrNull() ?: return null
        return AuthSession(accessToken = access, refreshToken = refresh, user = user)
    }

    private companion object {
        const val KEY_ACCESS = "auth_access_token"
        const val KEY_REFRESH = "auth_refresh_token"
        const val KEY_USER = "auth_user_v1"
    }
}
