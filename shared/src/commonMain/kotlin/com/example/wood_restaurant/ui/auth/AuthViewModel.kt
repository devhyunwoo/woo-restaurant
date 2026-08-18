package com.example.wood_restaurant.ui.auth

import androidx.lifecycle.ViewModel
import com.example.wood_restaurant.data.AuthException
import com.example.wood_restaurant.data.AuthRepository
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container

class AuthViewModel(
    private val authRepository: AuthRepository,
) : ViewModel(), ContainerHost<AuthState, AuthSideEffect> {

    override val container = container<AuthState, AuthSideEffect>(AuthState())

    fun onEmailChanged(value: String) = intent {
        reduce { state.copy(email = value, errorMessage = null) }
    }

    fun onPasswordChanged(value: String) = intent {
        reduce { state.copy(password = value, errorMessage = null) }
    }

    fun onNicknameChanged(value: String) = intent {
        reduce { state.copy(nickname = value, errorMessage = null) }
    }

    fun onTogglePasswordVisibility() = intent {
        reduce { state.copy(isPasswordVisible = !state.isPasswordVisible) }
    }

    /** 로그인 ↔ 회원가입. 입력값은 유지해서 다시 안 치게 한다. */
    fun onToggleMode() = intent {
        val next = if (state.mode == AuthMode.LOGIN) AuthMode.SIGNUP else AuthMode.LOGIN
        reduce { state.copy(mode = next, errorMessage = null) }
    }

    fun onSubmit() = intent {
        if (!state.canSubmit) return@intent
        reduce { state.copy(isLoading = true, errorMessage = null) }

        val result = runCatching {
            when (state.mode) {
                AuthMode.LOGIN -> authRepository.login(state.email, state.password)
                AuthMode.SIGNUP -> authRepository.signup(state.email, state.password, state.nickname)
            }
        }

        val user = result.getOrNull()
        if (user == null) {
            val error = result.exceptionOrNull()
            val message = when (error) {
                is AuthException -> error.message
                else -> error?.message ?: "네트워크 오류가 발생했습니다"
            }
            reduce { state.copy(isLoading = false, errorMessage = message) }
            return@intent
        }

        // 비밀번호는 성공 즉시 상태에서 지운다.
        reduce { state.copy(isLoading = false, password = "") }
        postSideEffect(AuthSideEffect.LoggedIn(user.nickname))
    }
}
