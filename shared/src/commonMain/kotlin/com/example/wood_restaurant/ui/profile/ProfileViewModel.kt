package com.example.wood_restaurant.ui.profile

import androidx.lifecycle.ViewModel
import com.example.wood_restaurant.data.AuthRepository
import com.example.wood_restaurant.data.FavoritesRepository
import com.example.wood_restaurant.domain.UserProfile
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container

data class ProfileState(
    /** null 이면 로그아웃 상태. */
    val user: UserProfile? = null,
    val favoriteCount: Int = 0,
    /** false 면 백엔드 미설정(네이버 직접 호출 개발 모드) — 로그인 버튼 대신 안내를 띄운다. */
    val isAuthAvailable: Boolean = true,
    val isLoggingOut: Boolean = false,
)

sealed interface ProfileSideEffect {
    data class ShowMessage(val message: String) : ProfileSideEffect
}

class ProfileViewModel(
    private val authRepository: AuthRepository,
    private val favoritesRepository: FavoritesRepository,
) : ViewModel(), ContainerHost<ProfileState, ProfileSideEffect> {

    override val container = container<ProfileState, ProfileSideEffect>(
        ProfileState(
            user = authRepository.currentUser,
            favoriteCount = favoritesRepository.favorites.value.size,
            isAuthAvailable = authRepository.isAvailable,
        ),
        onCreate = {
            // 세션과 찜은 각자 저장소가 진실의 원천. 둘 다 구독한다.
            intent { authRepository.session.collect { s -> reduce { state.copy(user = s?.user) } } }
            intent { favoritesRepository.favorites.collect { f -> reduce { state.copy(favoriteCount = f.size) } } }
        },
    )

    fun onLogout() = intent {
        if (state.user == null || state.isLoggingOut) return@intent
        reduce { state.copy(isLoggingOut = true) }
        authRepository.logout()
        reduce { state.copy(isLoggingOut = false) }
        postSideEffect(ProfileSideEffect.ShowMessage("로그아웃했어요"))
    }
}
