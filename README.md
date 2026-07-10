# EscapeRoomSuite

직원용 앱과 손님용 앱을 하나의 Android Studio 프로젝트/하나의 Git 저장소에서 관리하는 멀티 모듈 프로젝트입니다.

## 모듈

- `app-manager` — 직원용 앱 (`com.example.escaperoomtimer`)
- `app-display` — 손님용 앱 (`com.example.escaperoomdisplay`)

두 모듈은 각각 별도의 APK로 빌드됩니다.

## Android Studio에서 실행

상단 실행 구성에서 다음 중 하나를 선택합니다.

- `app-manager` : 직원용 앱
- `app-display` : 손님용 앱

## 정리한 항목

- 중복 Gradle Wrapper / Version Catalog / 프로젝트 설정 제거
- `.gradle`, `.idea`, `build`, `local.properties` 제거
- 자동 생성된 예제 테스트 제거
- 직원용 앱의 옛 손님 화면과 힌트 실행 코드 제거
- 힌트앱 실행 기능은 손님용 앱에만 유지

## 다음 단계

통신 기능을 추가할 때 공통 데이터 모델이 필요하면 `shared` Android/Kotlin 모듈을 추가할 수 있습니다.
