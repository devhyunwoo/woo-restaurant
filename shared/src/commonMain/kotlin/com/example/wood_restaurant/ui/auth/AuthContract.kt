package com.example.wood_restaurant.ui.auth

enum class AuthMode { LOGIN, SIGNUP }

data class AuthState(
    val mode: AuthMode = AuthMode.LOGIN,
    val email: String = "",
    val password: String = "",
    val nickname: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    /** 마지막 제출 실패 사유. 입력을 고치면 사라진다. */
    val errorMessage: String? = null,
) {
    /** 서버 검증(@Email, @Size)과 같은 규칙. 통과 못 하면 버튼을 비활성화해 왕복을 아낀다. */
    val isEmailValid: Boolean get() = email.trim().let { it.contains('@') && it.length in 3..255 }
    val isPasswordValid: Boolean get() = password.length in 8..72
    val isNicknameValid: Boolean get() = nickname.trim().length in 2..30

    val canSubmit: Boolean
        get() = !isLoading && isEmailValid && isPasswordValid && (mode == AuthMode.LOGIN || isNicknameValid)
}

sealed interface AuthSideEffect {
    /** 로그인/가입 성공. 화면은 뒤로 가거나 메인으로 넘어간다. */
    data class LoggedIn(val nickname: String) : AuthSideEffect
    data class ShowMessage(val message: String) : AuthSideEffect
}
