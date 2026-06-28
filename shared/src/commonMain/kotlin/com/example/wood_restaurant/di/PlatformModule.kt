package com.example.wood_restaurant.di

import org.koin.core.module.Module

/** 플랫폼별 구현(Settings 등)을 제공하는 Koin 모듈. */
expect val platformModule: Module
