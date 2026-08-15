package com.example.wood_restaurant

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.wood_restaurant.navigation.AppNavHost

@Composable
@Preview
fun App() {
    // 시스템 다크모드를 따른다. 지도 야간모드도 같은 값을 보고 켜진다.
    val colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()

    MaterialTheme(colorScheme = colorScheme) {
        AppNavHost(
            modifier = Modifier
                .safeContentPadding()
                .fillMaxSize(),
        )
    }
}
