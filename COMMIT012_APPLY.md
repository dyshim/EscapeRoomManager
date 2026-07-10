# Commit012 적용 안내

이 변경은 `shared` 모듈과 같은 Wi-Fi용 UDP 실시간 동기화 기반을 추가합니다.

## 적용
1. ZIP 안의 모든 파일을 EscapeRoomSuite 루트에 덮어씁니다.
2. Android Studio에서 Gradle Sync를 실행합니다.
3. `app-manager`와 `app-display`를 각각 빌드합니다.
4. 두 기기를 같은 Wi-Fi에 연결합니다.
5. 직원용 앱을 실행한 뒤 손님용 앱을 실행합니다.
6. 직원용 앱에서 방 타이머를 시작하면 손님용 앱에 방 이름과 시간이 표시됩니다.

## 현재 방 선택 규칙
손님용 앱은 진행 중인 방을 우선 표시합니다. 진행 중인 방이 없으면 일시정지된 방, 그다음 첫 번째 방을 표시합니다.
여러 손님 태블릿을 방별로 고정 연결하는 기능은 다음 커밋에서 추가하는 것이 안전합니다.

## 권장 커밋 메시지
`commit012 - Add shared module and local realtime timer sync`
