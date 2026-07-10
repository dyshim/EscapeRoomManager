# Commit015 적용

현재 프로젝트 루트에 `EscapeRoomSuite` 폴더 안의 파일을 같은 경로로 덮어쓰세요.

이번 수정본에서는 `app-display/MainActivity.kt`의 잘못된
`import androidx.compose.foundation.layout.weight`를 제거했습니다.
`Modifier.weight(...)`는 `Row`의 scope 확장 함수로 사용되므로 별도 import가 필요하지 않습니다.

적용 후:
1. File > Sync Project with Gradle Files
2. Build > Clean Project
3. Build > Rebuild Project
4. app-manager, app-display 각각 빌드
