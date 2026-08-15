package com.example.wood_restaurant.di

import android.content.Context
import com.example.wood_restaurant.location.AndroidLocationProvider
import com.example.wood_restaurant.location.LocationProvider
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single<Settings> {
        val context: Context = get()
        SharedPreferencesSettings(
            context.getSharedPreferences("woo_restaurant_settings", Context.MODE_PRIVATE)
        )
    }
    single<LocationProvider> { AndroidLocationProvider(get()) }
}
