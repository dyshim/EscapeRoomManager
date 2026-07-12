# Commit030 적용 안내

## 변경 내용
- 직원용 설정 화면에서 웹 관리자 PIN 변경
- 숫자 4~8자리 PIN 지원
- 최초 PIN `1234`
- PIN은 원문이 아니라 salt + SHA-256 해시로 저장
- 웹 API와 WebSocket 인증에 변경된 PIN 즉시 적용
- 로그인 5회 실패 시 30초 잠금
- 직원용 설정 화면에서 PIN을 `1234`로 초기화 가능
- 웹 로그인 화면에서 남은 시도 횟수와 잠금 시간 안내

## 적용
ZIP 안의 파일을 프로젝트 루트에 같은 경로로 덮어씁니다.

1. Sync Project with Gradle Files
2. Clean Project
3. Rebuild Project
4. app-manager를 다시 실행
5. 직원용 앱 설정 → 웹 관리자 PIN에서 변경

기존 웹 브라우저에 저장된 PIN이 있다면 변경 후 자동으로 인증이 실패하며 로그인 화면으로 돌아갑니다. 새 PIN을 입력하면 됩니다.
