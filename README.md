This is a Kotlin Multiplatform project targeting Android, iOS.

* [/iosApp](./iosApp/iosApp) contains an iOS application. Even if you’re sharing your UI with Compose Multiplatform,
  you need this entry point for your iOS app. This is also where you should add SwiftUI code for your project.

* [/shared](./shared/src) is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
  - [commonMain](./shared/src/commonMain/kotlin) is for code that’s common for all targets.
  - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
    For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
    the [iosMain](./shared/src/iosMain/kotlin) folder would be the right place for such calls.
    Similarly, if you want to edit the Desktop (JVM) specific part, the [jvmMain](./shared/src/jvmMain/kotlin)
    folder is the appropriate location.

## 네이버 지도 / API 키 설정

홈 화면은 네이버 지도 위에 주변 식당·카페·빵집을 띄운다. 키가 없으면 지도는 회색으로만 뜨고
목록은 비어 있으니, 실행 전에 아래를 먼저 채운다.

### 1. `local.properties` (커밋 금지)

```properties
# 네이버 클라우드 플랫폼 > Maps > 애플리케이션 등록에서 발급 (Android/iOS 지도 SDK 공통)
naver.ncpKeyId=여기에_Key_ID

# 네이버 개발자센터(developers.naver.com) > 애플리케이션 > 검색 API
naver.openapi.clientId=여기에_Client_ID
naver.openapi.clientSecret=여기에_Client_Secret

# (선택) 리버스 지오코딩. 없으면 지역명 없이 검색해서 정확도가 떨어진다.
naver.ncp.apiKeyId=
naver.ncp.apiKey=
```

`shared/build.gradle.kts`의 `generateSecretKeys` 태스크가 이 값을 읽어
`SecretKeys.kt`를 생성한다. 환경변수(`NAVER_NCP_KEY_ID` 등)로도 넣을 수 있다.

> ⚠️ 검색 오픈API의 Client Secret이 앱 바이너리에 들어간다. 실서비스에서는 자체 서버를
> 프록시로 두고 클라이언트에서는 시크릿을 빼는 게 맞다.

### 2. iOS

`iosApp/Configuration/Config.xcconfig`의 `NAVER_NCP_KEY_ID`에 위와 같은 값을 넣는다.
(`Info.plist`의 `NMFNcpKeyId`로 주입된다.)

### 데이터 소스의 한계

- 네이버 **지역검색 API는 평점·리뷰수를 제공하지 않는다.** 별점순/리뷰순 정렬을 눈으로
  확인할 수 있도록 지금은 `StubRatingSource`(장소 id 기반 결정론적 더미값)를 쓴다.
  실제 값이 생기면 `di/Koin.kt`에서 `RatingSource` 구현만 갈아끼우면 된다.
  더미값을 끄려면 `EmptyRatingSource`로 바꾼다 → UI에 "평점 정보 없음"으로 표시된다.
- 지역검색은 **한 질의당 최대 5건**, 좌표·반경 파라미터가 없다. `PlaceRepository`가
  카테고리별 키워드로 질의를 쪼개 병렬 호출하고, 반경은 좌표 거리로 직접 걸러낸다.

## Running the apps

Use the run configurations provided by the run widget in your IDE's toolbar. You can also use these commands and options:

- Android app: `./gradlew :androidApp:assembleDebug`
- iOS app: CocoaPods를 쓰므로 **`iosApp/iosApp.xcworkspace`** 를 Xcode로 연다
  (`.xcodeproj`가 아니다). Pods는 Gradle sync 때 `pod install`이 자동으로 돈다.

### Running tests

Use the run button in your IDE's editor gutter, or run tests using Gradle tasks:

- Android tests: `./gradlew :shared:testAndroidHostTest`
- iOS tests: `./gradlew :shared:iosSimulatorArm64Test`

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…