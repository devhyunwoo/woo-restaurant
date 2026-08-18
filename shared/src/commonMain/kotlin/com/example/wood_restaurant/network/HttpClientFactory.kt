package com.example.wood_restaurant.network

import com.example.wood_restaurant.config.SecretKeys
import com.example.wood_restaurant.data.AuthSession
import com.example.wood_restaurant.data.AuthTokenStore
import com.example.wood_restaurant.data.remote.RefreshRequest
import com.example.wood_restaurant.data.remote.TokenResponse
import de.jensklingenberg.ktorfit.Ktorfit
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/** 네이버 검색 오픈API (developers.naver.com). */
const val NAVER_OPENAPI_BASE_URL = "https://openapi.naver.com/"

/** 네이버 클라우드 플랫폼 Maps API (지오코딩/리버스 지오코딩). */
const val NCP_MAPS_BASE_URL = "https://naveropenapi.apigw.ntruss.com/"

private val woodServerBaseUrl: String
    get() = SecretKeys.SERVER_BASE_URL.trimEnd('/') + "/"

/**
 * 우리 백엔드(woodrestaurant-server).
 *
 * 로그인돼 있으면 모든 요청에 `Authorization: Bearer <access>` 를 붙이고(sendWithoutRequest),
 * 401 이 오면 refresh 토큰으로 한 번 재발급 받아 재시도한다. 재발급마저 401 이면(폐기·만료) 세션을 지워
 * 앱이 로그아웃 상태가 되게 한다 — 네트워크 오류로 실패한 경우엔 세션을 남겨 둔다.
 */
fun createWoodServerHttpClient(tokenStore: AuthTokenStore): HttpClient = HttpClient {
    install(ContentNegotiation) { json(jsonConfig) }
    install(Logging) { level = LogLevel.INFO }
    // refresh 호출은 Ktorfit 을 거치지 않으므로(플러그인 내부) 상대 경로를 풀 base URL 이 필요하다.
    if (SecretKeys.isServerConfigured) {
        defaultRequest { url(woodServerBaseUrl) }
    }
    install(Auth) {
        bearer {
            loadTokens {
                tokenStore.current?.let { BearerTokens(it.accessToken, it.refreshToken) }
            }
            // 서버가 401 에 WWW-Authenticate 를 안 보내도, provider 가 하나뿐이면 Ktor 는 refresh 를 시도한다.
            refreshTokens {
                val session = tokenStore.current ?: return@refreshTokens null
                val response = client.post("api/v1/auth/refresh") {
                    markAsRefreshTokenRequest()
                    contentType(ContentType.Application.Json)
                    setBody(RefreshRequest(session.refreshToken))
                }
                when {
                    response.status.isSuccess() -> {
                        val token = response.body<TokenResponse>()
                        tokenStore.save(
                            AuthSession(
                                accessToken = token.accessToken,
                                refreshToken = token.refreshToken,
                                user = token.user.toDomain(),
                            ),
                        )
                        BearerTokens(token.accessToken, token.refreshToken)
                    }

                    response.status == HttpStatusCode.Unauthorized -> {
                        tokenStore.clear()
                        null
                    }

                    else -> null
                }
            }
            // 장소 검색은 공개 API 라 401 챌린지가 없다. 로그인돼 있으면 처음부터 토큰을 실어 보낸다.
            sendWithoutRequest { true }
        }
    }
}

fun createWoodServerKtorfit(httpClient: HttpClient): Ktorfit = Ktorfit.Builder()
    .httpClient(httpClient)
    // Ktorfit은 baseUrl이 반드시 "/"로 끝나야 한다. local.properties에 빼먹어도 되게 여기서 보정.
    .baseUrl(woodServerBaseUrl)
    .build()

private val jsonConfig = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

/**
 * 지역검색 전용 클라이언트.
 * 인증 헤더가 요청마다 붙으므로 다른 호스트와 클라이언트를 섞지 않는다(시크릿 유출 방지).
 */
fun createNaverOpenApiHttpClient(): HttpClient = HttpClient {
    install(ContentNegotiation) { json(jsonConfig) }
    install(Logging) { level = LogLevel.INFO }
    defaultRequest {
        header("X-Naver-Client-Id", SecretKeys.NAVER_OPENAPI_CLIENT_ID)
        header("X-Naver-Client-Secret", SecretKeys.NAVER_OPENAPI_CLIENT_SECRET)
    }
}

/** 리버스 지오코딩 전용 클라이언트. NCP API Gateway 인증 헤더를 사용한다. */
fun createNcpMapsHttpClient(): HttpClient = HttpClient {
    install(ContentNegotiation) { json(jsonConfig) }
    install(Logging) { level = LogLevel.INFO }
    defaultRequest {
        header("x-ncp-apigw-api-key-id", SecretKeys.NAVER_NCP_API_KEY_ID)
        header("x-ncp-apigw-api-key", SecretKeys.NAVER_NCP_API_KEY)
    }
}

fun createNaverOpenApiKtorfit(httpClient: HttpClient): Ktorfit = Ktorfit.Builder()
    .httpClient(httpClient)
    .baseUrl(NAVER_OPENAPI_BASE_URL)
    .build()

fun createNcpMapsKtorfit(httpClient: HttpClient): Ktorfit = Ktorfit.Builder()
    .httpClient(httpClient)
    .baseUrl(NCP_MAPS_BASE_URL)
    .build()
