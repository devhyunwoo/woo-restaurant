package com.example.wood_restaurant.data.remote

import com.example.wood_restaurant.domain.UserProfile
import de.jensklingenberg.ktorfit.http.Body
import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.Headers
import de.jensklingenberg.ktorfit.http.POST
import io.ktor.client.statement.HttpResponse
import kotlinx.serialization.Serializable

// ---- 요청 (서버 AuthDtos.kt 와 키가 1:1) ----

@Serializable
data class SignupRequest(val email: String, val password: String, val nickname: String)

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class RefreshRequest(val refreshToken: String)

@Serializable
data class LogoutRequest(val refreshToken: String)

// ---- 응답 ----

/** 서버 `UserResponse`. createdAt 은 ISO-8601 문자열이지만 앱에서 아직 안 써서 그대로 둔다. */
@Serializable
data class UserResponse(
    val id: Long,
    val email: String,
    val nickname: String,
    val createdAt: String? = null,
) {
    fun toDomain() = UserProfile(id = id, email = email, nickname = nickname)
}

/** 로그인/회원가입/재발급 공통 응답. */
@Serializable
data class TokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    /** access 토큰 수명(초). */
    val expiresIn: Long = 0,
    val user: UserResponse,
)

/** 서버 오류 본문(RFC 9457 Problem Detail, `application/problem+json`). 사용자에게는 [detail]을 보여준다. */
@Serializable
data class ProblemDetail(
    val title: String? = null,
    val status: Int? = null,
    val detail: String? = null,
)

/**
 * 인증 API. 응답을 [HttpResponse]로 받는 이유: 실패 시 서버가 `TokenResponse` 대신 [ProblemDetail]을 주므로
 * 상태 코드를 보고 본문을 골라 디코딩해야 한다. 그 분기는 [com.example.wood_restaurant.data.RemoteAuthRepository]에서 한다.
 *
 * `@Headers("Content-Type: application/json")` — Ktorfit 은 `@Body`만으로는 직렬화 대상 타입을 정하지 못한다.
 */
interface AuthApi {
    @Headers("Content-Type: application/json")
    @POST("api/v1/auth/signup")
    suspend fun signup(@Body body: SignupRequest): HttpResponse

    @Headers("Content-Type: application/json")
    @POST("api/v1/auth/login")
    suspend fun login(@Body body: LoginRequest): HttpResponse

    @Headers("Content-Type: application/json")
    @POST("api/v1/auth/refresh")
    suspend fun refresh(@Body body: RefreshRequest): HttpResponse

    /** Bearer 토큰은 HttpClient 의 Auth 플러그인이 붙인다. */
    @Headers("Content-Type: application/json")
    @POST("api/v1/auth/logout")
    suspend fun logout(@Body body: LogoutRequest): HttpResponse

    @GET("api/v1/users/me")
    suspend fun me(): HttpResponse
}
