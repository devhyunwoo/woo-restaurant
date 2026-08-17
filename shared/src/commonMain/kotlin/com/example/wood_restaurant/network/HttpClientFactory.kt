package com.example.wood_restaurant.network

import com.example.wood_restaurant.config.SecretKeys
import de.jensklingenberg.ktorfit.Ktorfit
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/** 네이버 검색 오픈API (developers.naver.com). */
const val NAVER_OPENAPI_BASE_URL = "https://openapi.naver.com/"

/** 네이버 클라우드 플랫폼 Maps API (지오코딩/리버스 지오코딩). */
const val NCP_MAPS_BASE_URL = "https://naveropenapi.apigw.ntruss.com/"

/** 우리 백엔드(woodrestaurant-server). 인증 헤더 없음. */
fun createWoodServerHttpClient(): HttpClient = HttpClient {
    install(ContentNegotiation) { json(jsonConfig) }
    install(Logging) { level = LogLevel.INFO }
}

fun createWoodServerKtorfit(httpClient: HttpClient): Ktorfit = Ktorfit.Builder()
    .httpClient(httpClient)
    // Ktorfit은 baseUrl이 반드시 "/"로 끝나야 한다. local.properties에 빼먹어도 되게 여기서 보정.
    .baseUrl(SecretKeys.SERVER_BASE_URL.trimEnd('/') + "/")
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
