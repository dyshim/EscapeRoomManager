# EscapeRoomSuite — Commit028

직원용 Android 앱(`app-manager`), 손님용 Android 앱(`app-display`), 공통 코드(`shared`)를 포함한 멀티 모듈 프로젝트입니다.

## Commit028 핵심 수정

- 직원용 앱 시작 시 로컬 웹 서버를 즉시 시작
- Foreground Service에서도 웹 서버 유지 및 자동 재시작
- 웹 서버를 `0.0.0.0:8080`에 명시적으로 바인딩
- 포트 또는 Wi-Fi 오류 발생 시 2초 간격 자동 재시도
- 직원용 화면에 웹 서버 실행 상태 표시
- A7 절전 상태에서도 서버와 타이머가 유지되도록 WakeLock/Wi-Fi Lock 적용
- 웹 종료 알람, 화면 강조, 탭 제목 변경, 자동 정지 시간 설정 유지
- `/health` 진단 주소 추가

## Android Studio에서 열기

1. 이 폴더를 Android Studio에서 엽니다.
2. Gradle Sync를 실행합니다.
3. `app-manager`와 `app-display`를 각각 Debug 빌드합니다.

## PC 웹 접속

1. A7 직원용 앱과 PC를 **같은 Wi-Fi**에 연결합니다.
2. A7에서 직원용 앱을 실행합니다.
3. 홈 화면에서 아래 두 문구를 확인합니다.
   - `PC 웹 주소 http://192.168.x.x:8080`
   - `웹 서버 실행 중 · 포트 8080`
4. PC 브라우저 주소창에 표시된 주소를 입력합니다.
5. 초기 PIN은 `1234`입니다.

### 빠른 진단

브라우저에서 아래 주소를 열었을 때 JSON이 나오면 서버 연결은 정상입니다.

`http://직원폰IP:8080/health`

예: `http://192.168.0.15:8080/health`

## 웹 접속이 안 될 때

- PC와 A7이 같은 공유기/Wi-Fi인지 확인
- 게스트 Wi-Fi 또는 AP 격리(Client Isolation) 기능이 켜져 있지 않은지 확인
- A7에서 VPN과 모바일 핫스팟을 잠시 끄고 테스트
- 직원용 앱 상단바 알림이 유지되는지 확인
- 앱 화면에 `웹 서버 실행 중`이 표시되는지 확인
- Windows 방화벽보다 먼저 공유기 기기 간 통신 차단 설정을 확인

> 브라우저 알림 API는 일반 HTTP LAN 주소에서 브라우저 정책상 제한될 수 있습니다. 웹 화면의 빨간 종료 표시, 알람음, 탭 제목 변경은 동작하며, Windows 알림은 브라우저가 허용하는 환경에서만 표시됩니다.

## 빌드 모듈

- 직원용: `app-manager`
- 손님용: `app-display`
- 공통 코드: `shared`

## 추천 커밋 메시지

`commit028 - Rebuild reliable local web manager server`
