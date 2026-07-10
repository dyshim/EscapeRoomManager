# Commit017 적용 방법

프로젝트 루트에 이 ZIP의 내용을 같은 경로로 덮어씁니다.

변경 파일:
- shared/.../TcpProtocol.kt
- app-manager/.../TimerManager.kt
- app-manager/.../ManagerTcpServer.kt
- app-display/.../DisplayTcpClient.kt
- app-display/.../DisplaySyncManager.kt
- app-display/.../MainActivity.kt

기능:
- 손님용 앱에 START 화면 추가
- START 누르면 TCP로 직원용 앱에 시작 요청
- 직원용 타이머가 실제로 시작됨
- 시작 응답이 방 상태로 돌아오면 START 버튼이 사라지고 남은 시간이 표시됨
- 카운트다운 없음
- 힌트 번호/사용 기록 UI 제거, 힌트 앱 열기 버튼만 유지
- 중복 START 요청 방지
- 연결되지 않으면 START 비활성화
- 디버그 테스트 모드에서도 START 흐름 테스트 가능

적용 후:
1. Gradle Sync
2. Build > Clean Project
3. Build > Rebuild Project
4. app-manager 실행
5. app-display에서 방 선택 후 START 테스트

추천 커밋 메시지:
commit017 - Add guest start request without countdown
