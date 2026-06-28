package com.example.wood_restaurant

import androidx.lifecycle.ViewModel

class GreetingViewModel(
    private val greeting: Greeting,
) : ViewModel() {
    fun greet(): String = greeting.greet()
}
