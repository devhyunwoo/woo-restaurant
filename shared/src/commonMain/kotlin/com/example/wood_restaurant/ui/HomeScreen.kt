package com.example.wood_restaurant.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.wood_restaurant.GreetingViewModel
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel

import woodrestaurant.shared.generated.resources.Res
import woodrestaurant.shared.generated.resources.compose_multiplatform

@Composable
fun HomeScreen(
    onPostClick: (Int) -> Unit,
    viewModel: GreetingViewModel = koinViewModel(),
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Image(painterResource(Res.drawable.compose_multiplatform), null)
        Text("Home: ${viewModel.greet()}")
        Button(onClick = { onPostClick(1) }) {
            Text("Go to detail #1")
        }
    }
}
