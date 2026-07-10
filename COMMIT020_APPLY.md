# Commit020 적용 방법

프로젝트 루트에 이 ZIP의 `app-display` 폴더를 그대로 덮어씁니다.

그다음 Android Studio에서:

1. Gradle Sync
2. Build > Clean Project
3. Build > Rebuild Project

## 동작

- 방을 한 번 선택하면 앱 재실행 후에도 같은 방으로 바로 진입합니다.
- 손님 화면에는 방 변경 버튼이 표시되지 않습니다.
- 방 이름을 3초 안에 5번 연속 누르면 관리자 PIN 창이 열립니다.
- 초기 관리자 PIN: `1234`
- PIN 확인 후 방 변경 또는 관리자 PIN 변경이 가능합니다.
- 변경한 PIN은 기기에 저장됩니다.
