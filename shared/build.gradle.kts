import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinCocoapods)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.ktorfit)
}

/**
 * API 키는 저장소에 커밋하지 않는다.
 * local.properties(우선) → 환경변수 순서로 읽어 [SecretKeys] 파일을 생성한다.
 *
 * local.properties 예시:
 *   naver.ncpKeyId=xxxxxxxx           # 지도 SDK(Android/iOS 공통)
 *   naver.openapi.clientId=xxxxxxxx   # 검색 오픈API (developers.naver.com)
 *   naver.openapi.clientSecret=xxxxxx
 *   naver.ncp.apiKeyId=xxxxxxxx       # 리버스 지오코딩 (선택, NCP 콘솔)
 *   naver.ncp.apiKey=xxxxxxxx
 */
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun secret(propertyKey: String, envKey: String): String =
    localProperties.getProperty(propertyKey) ?: providers.environmentVariable(envKey).orNull ?: ""

val naverNcpKeyId = secret("naver.ncpKeyId", "NAVER_NCP_KEY_ID")
val naverOpenApiClientId = secret("naver.openapi.clientId", "NAVER_OPENAPI_CLIENT_ID")
val naverOpenApiClientSecret = secret("naver.openapi.clientSecret", "NAVER_OPENAPI_CLIENT_SECRET")
val naverNcpApiKeyId = secret("naver.ncp.apiKeyId", "NAVER_NCP_API_KEY_ID")
val naverNcpApiKey = secret("naver.ncp.apiKey", "NAVER_NCP_API_KEY")

val generateSecretKeys by tasks.registering {
    val outputDir = layout.buildDirectory.dir("generated/secrets/kotlin")
    val ncpKeyId = naverNcpKeyId
    val clientId = naverOpenApiClientId
    val clientSecret = naverOpenApiClientSecret
    val ncpApiKeyId = naverNcpApiKeyId
    val ncpApiKey = naverNcpApiKey

    inputs.property("ncpKeyId", ncpKeyId)
    inputs.property("clientId", clientId)
    inputs.property("clientSecret", clientSecret)
    inputs.property("ncpApiKeyId", ncpApiKeyId)
    inputs.property("ncpApiKey", ncpApiKey)
    outputs.dir(outputDir)

    doLast {
        val packageDir = outputDir.get().asFile.resolve("com/example/wood_restaurant/config")
        packageDir.mkdirs()
        packageDir.resolve("SecretKeys.kt").writeText(
            """
            package com.example.wood_restaurant.config

            /** 빌드 시 local.properties/환경변수로부터 생성된다. 직접 수정하지 말 것. */
            object SecretKeys {
                const val NAVER_NCP_KEY_ID: String = "$ncpKeyId"
                const val NAVER_OPENAPI_CLIENT_ID: String = "$clientId"
                const val NAVER_OPENAPI_CLIENT_SECRET: String = "$clientSecret"
                const val NAVER_NCP_API_KEY_ID: String = "$ncpApiKeyId"
                const val NAVER_NCP_API_KEY: String = "$ncpApiKey"

                /** 지역검색 호출 가능 여부. */
                val isOpenApiConfigured: Boolean
                    get() = NAVER_OPENAPI_CLIENT_ID.isNotBlank() && NAVER_OPENAPI_CLIENT_SECRET.isNotBlank()

                /** 리버스 지오코딩 호출 가능 여부. 없으면 지역명 없이 검색한다. */
                val isReverseGeocodeConfigured: Boolean
                    get() = NAVER_NCP_API_KEY_ID.isNotBlank() && NAVER_NCP_API_KEY.isNotBlank()
            }

            """.trimIndent()
        )
    }
}

kotlin {
    iosArm64()
    iosSimulatorArm64()

    cocoapods {
        version = "1.0.0"
        summary = "Woodrestaurant shared module"
        homepage = "https://github.com/example/woodrestaurant"
        ios.deploymentTarget = "18.2"
        podfile = project.file("../iosApp/Podfile")

        framework {
            baseName = "Shared"
            isStatic = true
        }

        // 네이버 지도 iOS SDK. cinterop으로 NMFNaverMapView/NMFMarker 등을 Kotlin에서 직접 쓴다.
        pod("NMapsMap") {
            version = libs.versions.naverMapIos.get()
            extraOpts += listOf("-compiler-option", "-fmodules")
        }
        // NMapsGeometry는 NMapsMap의 의존 pod이라 CocoaPods가 알아서 링크한다.
        // NMGLatLng 같은 타입도 -fmodules 덕에 NMapsMap cinterop에 함께 들어오므로 따로 선언하지 않는다.
        // (따로 pod()을 걸면 같은 이름의 Kotlin 타입이 두 개 생겨 타입 불일치가 난다.)
    }

    androidLibrary {
       namespace = "com.example.wood_restaurant.shared"
       compileSdk = libs.versions.android.compileSdk.get().toInt()
       minSdk = libs.versions.android.minSdk.get().toInt()

       compilerOptions {
           jvmTarget = JvmTarget.JVM_11
       }
       androidResources {
           enable = true
       }
       withHostTest {
           isIncludeAndroidResources = true
       }
    }

    sourceSets {
        commonMain {
            kotlin.srcDir(generateSecretKeys)
        }
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.android)
            implementation(libs.ktor.client.okhttp)

            implementation(libs.naver.map.sdk)
            implementation(libs.naver.map.compose)
            implementation(libs.playServices.location)

            // 위치 권한 요청(rememberLauncherForActivityResult). androidx.core는 여기서 전이로 따라온다.
            implementation(libs.androidx.activity.compose)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.material.icons.core)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            implementation(libs.kotlinx.coroutines.core)

            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.koin.compose.viewmodel.navigation)

            implementation(libs.navigation.compose)

            implementation(libs.orbit.core)
            implementation(libs.orbit.viewmodel)
            implementation(libs.orbit.compose)

            implementation(libs.ktorfit.lib)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.contentNegotiation)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.serialization.json)
            implementation(libs.kotlinx.serialization.json)

            implementation(libs.multiplatform.settings)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.orbit.test)
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}

// KSP(ktorfit) 코드 생성이 SecretKeys.kt 생성보다 먼저 돌지 않도록 보장한다.
tasks.matching { it.name.startsWith("ksp") }.configureEach {
    dependsOn(generateSecretKeys)
}
