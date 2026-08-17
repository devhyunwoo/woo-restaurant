package com.example.wood_restaurant.ui.main

import androidx.lifecycle.ViewModel
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container

class MainViewModel : ViewModel(), ContainerHost<MainState, MainSideEffect> {

    override val container = container<MainState, MainSideEffect>(MainState())

    fun onTabSelected(tab: MainTab) = intent {
        if (state.selectedTab == tab) {
            postSideEffect(MainSideEffect.ShowMessage("이미 ${tab.label} 탭입니다"))
        } else {
            reduce { state.copy(selectedTab = tab) }
        }
    }

    /** 다른 화면에서 프로그램적으로 탭을 바꿀 때. 같은 탭이어도 조용히 넘어간다. */
    fun switchTo(tab: MainTab) = intent {
        reduce { state.copy(selectedTab = tab) }
    }
}
