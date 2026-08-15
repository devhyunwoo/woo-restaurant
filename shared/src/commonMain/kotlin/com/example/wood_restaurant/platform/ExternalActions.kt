package com.example.wood_restaurant.platform

import androidx.compose.runtime.Composable

/**
 * 앱 밖으로 나가는 동작 모음. 브라우저 · 전화 · 공유 시트 · 앱 설정.
 * 플랫폼별 인텐트/URL 스킴 차이를 여기서 흡수한다.
 */
interface ExternalActions {
    /** 패키지명(Android) 또는 번들 ID(iOS). 네이버 지도 URL 스킴의 appname에 넣는다. */
    val appIdentifier: String

    /** [url]을 연다. 못 열면(앱 미설치 등) [fallbackUrl]을 시도한다. */
    fun openUrl(url: String, fallbackUrl: String? = null)

    /** 전화 앱을 번호가 입력된 상태로 연다. 바로 걸지는 않는다. */
    fun dial(phoneNumber: String)

    fun share(text: String)

    /** 이 앱의 시스템 설정 화면(권한 등). */
    fun openAppSettings()
}

@Composable
expect fun rememberExternalActions(): ExternalActions
