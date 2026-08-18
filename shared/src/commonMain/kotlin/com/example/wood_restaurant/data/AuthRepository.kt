package com.example.wood_restaurant.data

import com.example.wood_restaurant.config.SecretKeys
import com.example.wood_restaurant.data.remote.AuthApi
import com.example.wood_restaurant.data.remote.LoginRequest
import com.example.wood_restaurant.data.remote.LogoutRequest
import com.example.wood_restaurant.data.remote.ProblemDetail
import com.example.wood_restaurant.data.remote.SignupRequest
import com.example.wood_restaurant.data.remote.TokenResponse
import com.example.wood_restaurant.domain.UserProfile
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.auth.authProvider
import io.ktor.client.plugins.auth.providers.BearerAuthProvider
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json

/** 사용자에게 그대로 보여줄 수 있는 메시지를 담은 인증 실패. */
class AuthException(message: String) : Exception(message)

interface AuthRepository {
    /** null 이면 로그아웃 상태. */
    val session: StateFlow<AuthSession?>
    val currentUser: UserProfile? get() = session.value?.user
    val isLoggedIn: Boolean get() = session.value != null

    /** 백엔드 주소가 설정돼 있어야 로그인이 가능하다(네이버 직접 호출 개발 모드에선 불가). */
    val isAvailable: Boolean

    /** 성공하면 세션을 저장하고 사용자 정보를 돌려준다. 실패는 [AuthException]. */
    suspend fun login(email: String, password: String): UserProfile
    suspend fun signup(email: String, password: String, nickname: String): UserProfile

    /** 서버 로그아웃은 최선 노력. 네트워크가 죽어 있어도 로컬 세션은 반드시 지운다. */
    suspend fun logout()
}

class RemoteAuthRepository(
    private val api: AuthApi,
    private val tokenStore: AuthTokenStore,
    /** Bearer 캐시를 비우기 위해 필요하다 — Auth 플러그인은 loadTokens 결과를 한 번 읽고 들고 있는다. */
    private val httpClient: HttpClient,
) : AuthRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override val session: StateFlow<AuthSession?> = tokenStore.session
    override val isAvailable: Boolean get() = SecretKeys.isServerConfigured

    override suspend fun login(email: String, password: String): UserProfile {
        requireServer()
        val response = api.login(LoginRequest(email = email.trim(), password = password))
        return storeSession(response)
    }

    override suspend fun signup(email: String, password: String, nickname: String): UserProfile {
        requireServer()
        val response = api.signup(SignupRequest(email = email.trim(), password = password, nickname = nickname.trim()))
        return storeSession(response)
    }

    override suspend fun logout() {
        val refresh = tokenStore.current?.refreshToken
        // 로컬부터 지운다 — 서버 호출이 실패해도 사용자는 "로그아웃됨"을 봐야 한다.
        tokenStore.clear()
        httpClient.authProvider<BearerAuthProvider>()?.clearToken()
        if (refresh != null && isAvailable) {
            runCatching { api.logout(LogoutRequest(refresh)) }
        }
    }

    private suspend fun storeSession(response: HttpResponse): UserProfile {
        if (!response.status.isSuccess()) throw AuthException(response.errorMessage())
        val token = response.body<TokenResponse>()
        tokenStore.save(
            AuthSession(
                accessToken = token.accessToken,
                refreshToken = token.refreshToken,
                user = token.user.toDomain(),
            ),
        )
        // 다음 요청부터 새 토큰을 쓰도록 Auth 플러그인의 캐시를 비운다.
        httpClient.authProvider<BearerAuthProvider>()?.clearToken()
        return token.user.toDomain()
    }

    /**
     * 실패 응답을 사람이 읽을 메시지로. 서버는 `application/problem+json` 을 주는데
     * ContentNegotiation 은 `application/json` 만 알아서 본문을 직접 파싱한다.
     */
    private suspend fun HttpResponse.errorMessage(): String {
        val problem = runCatching { json.decodeFromString<ProblemDetail>(bodyAsText()) }.getOrNull()
        return problem?.detail ?: problem?.title ?: "요청에 실패했습니다 (${status.value})"
    }

    private fun requireServer() {
        if (!isAvailable) throw AuthException("서버 주소가 설정되지 않아 로그인할 수 없습니다")
    }
}
