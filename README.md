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

## 홈 화면이 하는 일

- 현재 위치(권한 없으면 마지막 검색 위치 → 서울시청) 주변의 **식당 · 커피 · 빵집**을 네이버 지도에 마커로, 아래엔 목록으로 보여준다.
- 필터는 전부 AND로 겹쳐 걸린다: 카테고리(다중) · 정렬(가까운/별점/리뷰/이름) · 반경 · 최소 별점 · 키워드 · **찜만 보기**.
- 마커/행을 탭하면 하단에 **상세 카드**: 전화 · 길찾기(네이버 지도 앱, 없으면 웹) · 네이버 링크 · 공유 · 찜.
- **찜**은 기기에 저장되고, "찜만 보기"에선 검색 결과와 무관하게 항상 보인다(반경 무시).
- **🎲 오늘 뭐 먹지?** — 지금 보이는 목록에서 하나를 무작위로 골라 준다.
- 최근 검색어, 카테고리별 개수 뱃지, 도보 소요 시간, 지도 300m 이상 이동 시 "이 지역 재검색"(자동 재검색 스위치를 켜면 버튼 없이 바로).
- 마커는 SDK 클러스터러로 뭉치고(줌아웃 시 숫자 원), 선택한 장소만 위에 크게 따로 그린다.
- **찜 탭**: 카테고리별 필터, 스와이프 삭제, "지도에서 보기" → 홈이 찜만 보기 모드로 그 장소를 띄운다.
- 시스템 다크모드를 따르며 지도도 야간 모드로 바뀐다. 위치 권한이 있으면 파란 현재 위치 점이 뜬다.

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

### 백엔드 경유 (권장)

검색 오픈API의 Client Secret을 앱에 넣지 않으려면 [woodrestaurant-server](https://github.com/devhyunwoo/woodrestaurant-server)를
띄우고 `local.properties`에 주소만 적는다. 있으면 앱은 네이버 대신 서버를 부른다.

```properties
# Android 에뮬레이터 → 호스트 PC 는 10.0.2.2. iOS 시뮬레이터는 localhost. 실기기는 PC의 LAN IP.
server.baseUrl=http://10.0.2.2:8080/
```

이때 `naver.openapi.*` 는 앱에 없어도 된다(서버 쪽 `application-local.yml`에만).
디버그 빌드는 로컬 http를 허용해 뒀다(Android: debug 매니페스트 `usesCleartextTraffic`, iOS: `NSAllowsLocalNetworking`).

> ⚠️ 서버 없이 `naver.openapi.*` 를 앱에 넣는 방식은 개발용이다. Client Secret이 앱 바이너리에 들어가므로
> 스토어에 올리기 전에는 반드시 서버 경유로 바꾼다.

### 2. iOS

`iosApp/Configuration/Config.xcconfig`의 `NAVER_NCP_KEY_ID`에 위와 같은 값을 넣는다.
(`Info.plist`의 `NMFNcpKeyId`로 주입된다.)

### 데이터 소스의 한계

- 네이버 **지역검색 API는 평점·리뷰수를 제공하지 않는다.** 별점순/리뷰순 정렬을 눈으로
  확인할 수 있도록 지금은 `StubRatingSource`(장소 id 기반 결정론적 더미값)를 쓴다.
  실제 값이 생기면 `di/Koin.kt`에서 `RatingSource` 구현만 갈아끼우면 된다.
  더미값을 끄려면 `EmptyRatingSource`로 바꾼다 → UI에 "평점 정보 없음"으로 표시된다.
- 지역검색은 **한 질의당 최대 5건**, 좌표·반경 파라미터가 없다. `NaverPlaceRepository`가
  카테고리별 키워드로 질의를 쪼개 병렬 호출하고, 반경은 좌표 거리로 직접 걸러낸다.
- 일 호출 한도(25,000건)를 아끼기 위해 같은 지역(≈100m 격자)·카테고리·키워드는 5분간 캐시한다.

## Running the apps

Use the run configurations provided by the run widget in your IDE's toolbar. You can also use these commands and options:

- Android app: `./gradlew :androidApp:assembleDebug`
- iOS app: CocoaPods를 쓰므로 **`iosApp/iosApp.xcworkspace`** 를 Xcode로 연다
  (`.xcodeproj`가 아니다). Pods는 Gradle sync 때 `pod install`이 자동으로 돈다.

### Running tests

Use the run button in your IDE's editor gutter, or run tests using Gradle tasks:

- Android tests: `./gradlew :shared:testAndroidHostTest`
- iOS tests: `./gradlew :shared:iosSimulatorArm64Test`

도메인(필터·링크)과 `HomeViewModel`(Orbit test + 페이크 저장소)을 공통 테스트로 검증한다.

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…