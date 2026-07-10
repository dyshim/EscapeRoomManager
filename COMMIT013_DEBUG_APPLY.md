# Commit013 Debug test mode

덮어쓸 파일:

- `app-display/src/main/java/com/example/escaperoomdisplay/MainActivity.kt`
- `app-display/src/main/java/com/example/escaperoomdisplay/network/DisplaySyncManager.kt`

기능:

- Debug 빌드에서만 `테스트 데이터 불러오기` 버튼 표시
- 테스트용 방 3개 생성
- 진행 중 방의 시간이 1초마다 감소
- 5분 이하 빨간색 표시 확인 가능
- Release 빌드에서는 테스트 버튼과 테스트 종료 UI가 표시되지 않음
