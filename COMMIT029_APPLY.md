# Commit029 적용 안내

현재 프로젝트 루트에 이 ZIP의 `app-manager` 폴더를 같은 경로로 덮어씁니다.

## 변경 내용
- 웹 대시보드 상태 갱신을 1초 HTTP polling에서 WebSocket 실시간 push로 변경
- WebSocket 끊김 시 2초 자동 재연결
- WebSocket 장애 시 5초 HTTP fallback
- 웹 종료 알람, 브라우저 알림, 자동 정지 시간 유지
- +10초 / -10초 및 직접 시간 입력 유지
- 다크 모드와 PC/모바일 반응형 UI 개선
- PWA manifest, 아이콘, service worker 추가
- 직원용 서비스가 매초 웹 상태를 push

## PWA 참고
같은 Wi-Fi의 `http://192.168.x.x:8080` 접속은 HTTPS가 아니므로 Chrome 정책에 따라 정식 PWA 설치 버튼이 나타나지 않을 수 있습니다.
그 경우 Chrome/Edge 메뉴의 **바로가기 만들기** 또는 **앱으로 설치** 항목을 사용하세요.
WebSocket과 웹 관리 기능은 HTTP에서도 정상 동작합니다.

## 적용 후
1. Sync Project with Gradle Files
2. Clean Project
3. Rebuild Project
4. app-manager 재설치 및 실행
5. PC 브라우저에서 `http://직원폰IP:8080/` 접속
6. PIN `1234`

## 추천 커밋 메시지
`commit029 - Complete realtime web dashboard and add PWA assets`
