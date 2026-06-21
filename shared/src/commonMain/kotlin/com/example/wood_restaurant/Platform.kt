package com.example.wood_restaurant

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform