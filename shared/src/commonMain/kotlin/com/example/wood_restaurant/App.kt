package com.example.wood_restaurant

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.wood_restaurant.navigation.AppNavHost

@Composable
@Preview
fun App() {
    MaterialTheme {
        AppNavHost(
            modifier = Modifier
                .safeContentPadding()
                .fillMaxSize(),
        )
    }
}
