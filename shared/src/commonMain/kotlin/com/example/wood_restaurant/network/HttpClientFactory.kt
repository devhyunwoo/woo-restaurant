package com.example.wood_restaurant.network

import de.jensklingenberg.ktorfit.Ktorfit
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

const val BASE_URL = "https://jsonplaceholder.typicode.com/"

fun createHttpClient(): HttpClient = HttpClient {
    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
                isLenient = true
            }
        )
    }
    install(Logging) {
        level = LogLevel.INFO
    }
}

fun createKtorfit(httpClient: HttpClient): Ktorfit = Ktorfit.Builder()
    .httpClient(httpClient)
    .baseUrl(BASE_URL)
    .build()
