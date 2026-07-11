package com.example.wood_restaurant.ui.main

/**
 * 메인 화면 MVI 컨트랙트.
 * Orbit은 State/SideEffect 두 타입만 요구하며(ContainerHost<State, SideEffect>),
 * Intent는 별도 sealed 클래스 없이 ViewModel의 함수로 표현한다.
 */
enum class MainTab(val label: String) {
    HOME("홈"),
    SEARCH("검색"),
    ORDERS("주문"),
    PROFILE("마이"),
}

data class MainState(
    val selectedTab: MainTab = MainTab.HOME,
)

sealed interface MainSideEffect {
    data class ShowMessage(val message: String) : MainSideEffect
}
