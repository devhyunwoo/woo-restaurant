package com.example.wood_restaurant.di

import com.example.wood_restaurant.location.IosLocationProvider
import com.example.wood_restaurant.location.LocationProvider
import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSUserDefaults

actual val platformModule: Module = module {
    single<Settings> {
        NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults)
    }
    single<LocationProvider> { IosLocationProvider() }
}
