# Commit021 적용 방법

프로젝트 루트에 압축 내용을 덮어쓴 뒤 아래 순서로 진행하세요.

1. File → Sync Project with Gradle Files
2. Build → Clean Project
3. Build → Rebuild Project
4. app-manager 실행
5. 설정 화면에서 테마 프리셋을 추가하고 각 방에 적용

추가/변경 파일:

- `app-manager/src/main/java/com/example/escaperoomtimer/model/ThemePreset.kt`
- `app-manager/src/main/java/com/example/escaperoomtimer/repository/ThemePresetRepository.kt`
- `app-manager/src/main/java/com/example/escaperoomtimer/ui/setting/SettingScreen.kt`

프리셋을 방에 적용하면 기존 `TimerManager.updateRoomSetting()`을 통해 방 이름과 기본 시간이 갱신됩니다. 직원용 서비스의 기존 TCP 브로드캐스트, 알림, 위젯 갱신 흐름을 그대로 사용하므로 손님용 앱과 위젯에도 자동 반영됩니다.
