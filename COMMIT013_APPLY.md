# Commit013 적용 안내

## 변경 내용
- 손님용 태블릿을 특정 방에 고정하는 선택 화면 추가
- 선택한 방 ID를 SharedPreferences에 저장하여 앱 재실행 후 유지
- 여러 방이 동시에 실행되어도 다른 방으로 자동 전환되지 않음
- 직원용 앱 신호가 5초 이상 끊기면 연결 끊김 표시
- 손님 화면에서 방 변경 기능 추가

## 변경 파일
- `app-display/src/main/java/com/example/escaperoomdisplay/MainActivity.kt`
- `app-display/src/main/java/com/example/escaperoomdisplay/network/DisplaySyncManager.kt`

## 적용 후
1. Gradle Sync
2. `app-manager`, `app-display` 빌드
3. 두 앱을 같은 Wi-Fi에 연결
4. 직원용 앱 실행
5. 손님용 앱에서 사용할 방 선택
