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
}
