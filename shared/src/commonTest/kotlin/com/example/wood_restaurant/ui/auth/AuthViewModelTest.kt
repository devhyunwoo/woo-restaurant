package com.example.wood_restaurant.ui.auth

import com.example.wood_restaurant.data.AuthException
import com.example.wood_restaurant.data.AuthRepository
import com.example.wood_restaurant.data.AuthSession
import com.example.wood_restaurant.domain.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.orbitmvi.orbit.test.test
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AuthViewModelTest {

    private class FakeAuthRepository(
        var error: Throwable? = null,
    ) : AuthRepository {
        private val _session = MutableStateFlow<AuthSession?>(null)
        override val session: StateFlow<AuthSession?> = _session
        override val isAvailable: Boolean = true

        val loginCalls = mutableListOf<Pair<String, String>>()
        val signupCalls = mutableListOf<Triple<String, String, String>>()

        private fun user(email: String, nickname: String) = UserProfile(1, email, nickname).also {
            _session.value = AuthSession("access", "refresh", it)
        }

        override suspend fun login(email: String, password: String): UserProfile {
            loginCalls += email to password
            error?.let { throw it }
            return user(email, "우드")
        }

        override suspend fun signup(email: String, password: String, nickname: String): UserProfile {
            signupCalls += Triple(email, password, nickname)
            error?.let { throw it }
            return user(email, nickname)
        }

        override suspend fun logout() {
            _session.value = null
        }
    }

    @Test
    fun `유효성 - 이메일 형식과 비밀번호 8자 이상이어야 제출 가능`() {
        val base = AuthState()
        assertFalse(base.canSubmit)
        assertFalse(base.copy(email = "a@b.c", password = "1234567").canSubmit)
        assertTrue(base.copy(email = "a@b.c", password = "12345678").canSubmit)
        assertFalse(base.copy(email = "no-at", password = "12345678").canSubmit)
    }

    @Test
    fun `유효성 - 회원가입 모드에선 닉네임 2자 이상도 필요`() {
        val s = AuthState(mode = AuthMode.SIGNUP, email = "a@b.c", password = "12345678")
        assertFalse(s.canSubmit)
        assertFalse(s.copy(nickname = "우").canSubmit)
        assertTrue(s.copy(nickname = "우드").canSubmit)
    }

    @Test
    fun `로그인 성공하면 비밀번호를 지우고 LoggedIn 을 보낸다`() = runTest {
        val repository = FakeAuthRepository()
        val viewModel = AuthViewModel(repository)

        viewModel.test(this) {
            expectInitialState()
            containerHost.onEmailChanged("a@b.c")
            expectState { copy(email = "a@b.c") }
            containerHost.onPasswordChanged("12345678")
            expectState { copy(password = "12345678") }

            containerHost.onSubmit()
            expectState { copy(isLoading = true) }
            expectState { copy(isLoading = false, password = "") }
            expectSideEffect(AuthSideEffect.LoggedIn("우드"))
        }
        assertEquals(listOf("a@b.c" to "12345678"), repository.loginCalls)
    }

    @Test
    fun `로그인 실패하면 서버 메시지를 그대로 보여준다`() = runTest {
        val repository = FakeAuthRepository(error = AuthException("이메일 또는 비밀번호가 올바르지 않습니다"))
        val viewModel = AuthViewModel(repository)

        viewModel.test(this) {
            expectInitialState()
            containerHost.onEmailChanged("a@b.c")
            expectState { copy(email = "a@b.c") }
            containerHost.onPasswordChanged("wrongpass")
            expectState { copy(password = "wrongpass") }

            containerHost.onSubmit()
            expectState { copy(isLoading = true) }
            expectState { copy(isLoading = false, errorMessage = "이메일 또는 비밀번호가 올바르지 않습니다") }

            // 입력을 고치면 에러가 사라진다.
            containerHost.onPasswordChanged("wrongpass2")
            expectState { copy(password = "wrongpass2", errorMessage = null) }
        }
    }

    @Test
    fun `회원가입 모드로 바꾸면 signup 을 호출한다`() = runTest {
        val repository = FakeAuthRepository()
        val viewModel = AuthViewModel(repository)

        viewModel.test(this) {
            expectInitialState()
            containerHost.onToggleMode()
            expectState { copy(mode = AuthMode.SIGNUP) }
            containerHost.onEmailChanged("new@b.c")
            expectState { copy(email = "new@b.c") }
            containerHost.onPasswordChanged("12345678")
            expectState { copy(password = "12345678") }
            containerHost.onNicknameChanged("새싹")
            expectState { copy(nickname = "새싹") }

            containerHost.onSubmit()
            expectState { copy(isLoading = true) }
            expectState { copy(isLoading = false, password = "") }
            expectSideEffect(AuthSideEffect.LoggedIn("새싹"))
        }
        assertEquals(listOf(Triple("new@b.c", "12345678", "새싹")), repository.signupCalls)
        assertTrue(repository.loginCalls.isEmpty())
    }

    @Test
    fun `유효하지 않으면 제출해도 아무 일도 없다`() = runTest {
        val repository = FakeAuthRepository()
        val viewModel = AuthViewModel(repository)

        viewModel.test(this) {
            expectInitialState()
            containerHost.onSubmit()
        }
        assertTrue(repository.loginCalls.isEmpty())
    }
}
