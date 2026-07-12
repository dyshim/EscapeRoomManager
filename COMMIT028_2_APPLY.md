# Commit028.2 웹 리팩터링 적용

프로젝트 루트에 이 ZIP의 `app-manager` 폴더를 그대로 덮어쓰세요.

변경 파일:
- `app-manager/src/main/java/com/example/escaperoomtimer/web/ManagerWebServer.kt`
- `app-manager/src/main/java/com/example/escaperoomtimer/MainActivity.kt`
- `app-manager/src/main/java/com/example/escaperoomtimer/service/TimerForegroundService.kt`
- `app-manager/src/main/assets/web/index.html`
- `app-manager/src/main/assets/web/style.css`
- `app-manager/src/main/assets/web/app.js`

적용 후:
1. Sync Project with Gradle Files
2. Clean Project
3. Rebuild Project
4. 기존 직원용 앱 삭제 후 `app-manager` 재설치
5. 브라우저의 기존 탭을 닫고 다시 접속
6. PIN `1234` 입력

진단 주소:
- `/health` : 웹 서버 상태
- `/app.js` : 분리된 JavaScript가 정상 제공되는지 확인

쿼리 문자열이 붙은 `/ ?v=...` 요청도 루트 화면으로 처리합니다.
