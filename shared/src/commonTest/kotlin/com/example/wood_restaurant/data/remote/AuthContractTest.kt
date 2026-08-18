package com.example.wood_restaurant.data.remote

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 백엔드 인증 API 응답 계약. 서버 `AuthDtos.kt` / `GlobalExceptionHandler` 형식 그대로.
 * 서버가 필드를 바꾸면 여기서 먼저 깨져야 한다.
 */
class AuthContractTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `TokenResponse 를 디코딩한다`() {
        val raw = """
            {
              "accessToken": "eyJ.access",
              "refreshToken": "eyJ.refresh",
              "tokenType": "Bearer",
              "expiresIn": 1800,
              "user": { "id": 42, "email": "a@b.c", "nickname": "우드", "createdAt": "2026-08-18T09:00:00Z" }
            }
        """.trimIndent()

        val decoded = json.decodeFromString<TokenResponse>(raw)
        assertEquals("eyJ.access", decoded.accessToken)
        assertEquals("eyJ.refresh", decoded.refreshToken)
        assertEquals(1800, decoded.expiresIn)
        assertEquals(42, decoded.user.id)
        assertEquals("우드", decoded.user.toDomain().nickname)
    }

    @Test
    fun `Problem Detail 에서 detail 을 읽는다`() {
        val raw = """
            {
              "type": "about:blank",
              "title": "로그인 실패",
              "status": 401,
              "detail": "이메일 또는 비밀번호가 올바르지 않습니다",
              "instance": "/api/v1/auth/login"
            }
        """.trimIndent()

        val decoded = json.decodeFromString<ProblemDetail>(raw)
        assertEquals(401, decoded.status)
        assertEquals("이메일 또는 비밀번호가 올바르지 않습니다", decoded.detail)
    }

    @Test
    fun `요청 DTO 키는 서버 파라미터명과 같다`() {
        assertEquals(
            """{"email":"a@b.c","password":"pw","nickname":"우드"}""",
            Json.encodeToString(SignupRequest("a@b.c", "pw", "우드")),
        )
        assertEquals("""{"refreshToken":"r"}""", Json.encodeToString(RefreshRequest("r")))
    }
}
