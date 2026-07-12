# Commit028.1 적용 방법

1. ZIP 압축을 풉니다.
2. 프로젝트 루트에 `app-manager` 폴더를 덮어씁니다.
3. Android Studio에서 `Sync Project with Gradle Files`를 실행합니다.
4. `Clean Project` 후 `Rebuild Project`를 실행합니다.
5. app-manager를 재실행합니다.
6. 브라우저에서 `http://직원폰IP:8080`에 접속하고 PIN `1234`를 입력합니다.

## 수정 내용
- 로그인 버튼과 `id="login"`의 이름 충돌 제거
- 서버 검증용 `POST /api/login` 추가
- 잘못된 PIN 오류 표시
- Enter 키 로그인 지원
- 로그인 요청 중 버튼 중복 클릭 방지
- JavaScript 예외 처리 및 저장된 PIN 재검증
