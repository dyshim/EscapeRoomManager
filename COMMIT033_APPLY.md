# Commit033 적용 안내

프로젝트 루트에 압축 내용을 같은 경로로 덮어쓰세요.

변경 파일:
- app-display/src/main/java/com/example/escaperoomdisplay/network/DisplaySyncManager.kt
- app-manager/src/main/java/com/example/escaperoomtimer/service/TimerForegroundService.kt
- app-manager/src/main/assets/web/app.js

변경 내용:
- 타이머 1초 ticker와 네트워크 heartbeat를 별도 Runnable로 분리
- 손님용 앱에서 연결 확인 시각 State 갱신을 3초 간격으로 제한
- 선택된 방 타이머만 매초 갱신하고 방 목록 전체 갱신을 최소화
- 웹 대시보드에서 매초 전체 HTML 재생성을 제거
- 웹은 타이머/종료 예정 텍스트만 부분 갱신
- 방 상태, 방 이름, 시작/정지 상태가 바뀔 때만 해당 목록을 다시 렌더링
- 동일한 연결 상태 문구를 반복해서 DOM에 쓰지 않음

적용 후:
1. Sync Project with Gradle Files
2. Clean Project
3. Rebuild Project
4. app-manager와 app-display 재설치
5. 웹 브라우저는 기존 탭을 닫고 다시 접속

참고: 이 변경은 UI 끊김 감소를 위한 구조 개선입니다. 손님용 START 문제는 별도 진단 후 다음 수정에서 처리합니다.
