# Commit027 적용 안내

## 추가/변경 파일

- `app-manager/src/main/java/com/example/escaperoomtimer/web/ManagerWebServer.kt` (신규)
- `app-manager/src/main/java/com/example/escaperoomtimer/service/TimerForegroundService.kt`
- `app-manager/src/main/java/com/example/escaperoomtimer/ui/home/HomeScreen.kt`

## 사용 방법

1. 직원용 A7과 PC를 같은 Wi-Fi에 연결합니다.
2. 직원용 앱을 실행합니다.
3. 홈 화면에 표시되는 `PC 웹 주소`를 PC 브라우저에 입력합니다.
   - 예: `http://192.168.0.15:8080`
4. 초기 PIN `1234`를 입력합니다.
5. 웹에서 방별 시작, 일시정지, 종료, ±5분, ±10초, 초기화, 직접 시간 입력을 사용할 수 있습니다.

## 주의

- A7의 직원용 앱이 실행 중이어야 웹 페이지도 열립니다.
- 같은 Wi-Fi 내부에서만 사용하는 로컬 웹 기능입니다.
- 포트는 `8080`입니다.
- 최초 버전의 웹 PIN은 `1234`로 고정되어 있습니다. 추후 설정 화면에서 변경할 수 있도록 확장할 수 있습니다.
