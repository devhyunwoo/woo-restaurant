package com.example.wood_restaurant.data

import com.example.wood_restaurant.domain.UserProfile
import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AuthTokenStoreTest {

    private val session = AuthSession("access-1", "refresh-1", UserProfile(7, "a@b.c", "우드"))

    @Test
    fun `저장한 세션은 새 인스턴스에서도 읽힌다`() {
        val settings = MapSettings()
        AuthTokenStore(settings).save(session)

        val reloaded = AuthTokenStore(settings)
        assertEquals(session, reloaded.current)
        assertEquals(session, reloaded.session.value)
    }

    @Test
    fun `clear 하면 세션이 없어지고 저장소도 비워진다`() {
        val settings = MapSettings()
        val store = AuthTokenStore(settings)
        store.save(session)
        store.clear()

        assertNull(store.current)
        assertNull(AuthTokenStore(settings).current)
    }

    @Test
    fun `사용자 JSON 이 깨져 있으면 로그아웃 상태로 시작한다`() {
        val settings = MapSettings()
        settings.putString("auth_access_token", "a")
        settings.putString("auth_refresh_token", "r")
        settings.putString("auth_user_v1", "{not json")

        assertNull(AuthTokenStore(settings).current)
    }
}
