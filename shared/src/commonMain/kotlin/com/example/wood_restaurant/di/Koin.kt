package com.example.wood_restaurant.di

import com.example.wood_restaurant.Greeting
import com.example.wood_restaurant.GreetingViewModel
import com.example.wood_restaurant.data.AppPreferences
import com.example.wood_restaurant.data.RestaurantRepository
import com.example.wood_restaurant.data.remote.RestaurantApi
import com.example.wood_restaurant.data.remote.createRestaurantApi
import com.example.wood_restaurant.network.createHttpClient
import com.example.wood_restaurant.network.createKtorfit
import com.example.wood_restaurant.ui.main.MainViewModel
import de.jensklingenberg.ktorfit.Ktorfit
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

val networkModule = module {
    single { createHttpClient() }
    single { createKtorfit(get()) }
    single<RestaurantApi> { get<Ktorfit>().createRestaurantApi() }
    singleOf(::RestaurantRepository)
}

val appModule = module {
    includes(platformModule, networkModule)
    singleOf(::AppPreferences)
    singleOf(::Greeting)
    viewModelOf(::GreetingViewModel)
    viewModelOf(::MainViewModel)
}

/**
 * 모든 플랫폼 공통 진입점.
 * Android에서는 androidContext 등을 넘기기 위해 appDeclaration을 사용한다.
 */
fun initKoin(appDeclaration: KoinAppDeclaration = {}) {
    startKoin {
        appDeclaration()
        modules(appModule)
    }
}
