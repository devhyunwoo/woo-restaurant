rootProject.name = "Woodrestaurant"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        // 네이버 지도 안드로이드 SDK(com.naver.maps:map-sdk)는 메이븐 센트럴이 아닌 자체 저장소에만 있다.
        maven("https://repository.map.naver.com/archive/maven") {
            mavenContent {
                includeGroupAndSubgroups("com.naver")
            }
        }
    }
}

include(":androidApp")
include(":shared")