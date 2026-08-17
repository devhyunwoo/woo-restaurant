package com.example.wood_restaurant.di

import com.example.wood_restaurant.data.AppPreferences
import com.example.wood_restaurant.data.FavoritesRepository
import com.example.wood_restaurant.data.NaverPlaceRepository
import com.example.wood_restaurant.data.PlaceRepository
import com.example.wood_restaurant.data.RatingSource
import com.example.wood_restaurant.data.RemotePlaceRepository
import com.example.wood_restaurant.data.StubRatingSource
import com.example.wood_restaurant.data.remote.NaverLocalApi
import com.example.wood_restaurant.data.remote.NaverReverseGeocodeApi
import com.example.wood_restaurant.data.remote.WoodServerApi
import com.example.wood_restaurant.data.remote.createNaverLocalApi
import com.example.wood_restaurant.data.remote.createNaverReverseGeocodeApi
import com.example.wood_restaurant.data.remote.createWoodServerApi
import com.example.wood_restaurant.network.createNaverOpenApiHttpClient
import com.example.wood_restaurant.network.createNaverOpenApiKtorfit
import com.example.wood_restaurant.network.createNcpMapsHttpClient
import com.example.wood_restaurant.network.createNcpMapsKtorfit
import com.example.wood_restaurant.network.createWoodServerHttpClient
import com.example.wood_restaurant.network.createWoodServerKtorfit
import com.example.wood_restaurant.config.SecretKeys
import com.example.wood_restaurant.ui.favorites.FavoritesViewModel
import com.example.wood_restaurant.ui.home.HomeViewModel
import com.example.wood_restaurant.ui.main.MainViewModel
import de.jensklingenberg.ktorfit.Ktorfit
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

private val NaverOpenApi = named("naverOpenApi")
private val NcpMaps = named("ncpMaps")
private val WoodServer = named("woodServer")

val networkModule = module {
    // 호스트마다 인증 헤더가 달라서 클라이언트를 분리한다.
    single(NaverOpenApi) { createNaverOpenApiHttpClient() }
    single(NaverOpenApi) { createNaverOpenApiKtorfit(get(NaverOpenApi)) }
    single<NaverLocalApi> { get<Ktorfit>(NaverOpenApi).createNaverLocalApi() }

    single(NcpMaps) { createNcpMapsHttpClient() }
    single(NcpMaps) { createNcpMapsKtorfit(get(NcpMaps)) }
    single<NaverReverseGeocodeApi> { get<Ktorfit>(NcpMaps).createNaverReverseGeocodeApi() }

    single(WoodServer) { createWoodServerHttpClient() }
    single(WoodServer) { createWoodServerKtorfit(get(WoodServer)) }
    single<WoodServerApi> { get<Ktorfit>(WoodServer).createWoodServerApi() }
}

val appModule = module {
    includes(platformModule, networkModule)
    singleOf(::AppPreferences)
    singleOf(::FavoritesRepository)

    // TODO: 자체 백엔드가 붙으면 실제 평점 소스로 교체한다. (EmptyRatingSource로 바꾸면 "정보 없음" 표시)
    single<RatingSource> { StubRatingSource }
    // 백엔드 주소가 있으면 서버 경유(시크릿이 앱에 없음), 없으면 네이버 직접 호출(개발용, 키 필요).
    single<PlaceRepository> {
        if (SecretKeys.isServerConfigured) RemotePlaceRepository(get(), get())
        else NaverPlaceRepository(get(), get(), get())
    }

    viewModelOf(::MainViewModel)
    viewModelOf(::HomeViewModel)
    viewModelOf(::FavoritesViewModel)
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
