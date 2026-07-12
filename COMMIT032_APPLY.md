# Commit032 적용 안내

프로젝트 루트에 이 ZIP의 파일을 같은 경로로 덮어쓰세요.

## 변경 내용
- 직원용/손님용 버전: `1.0.0-rc1` (`versionCode 32`)
- 직원용 앱 이름: `EscapeRoom Manager`
- 손님용 앱 이름: `EscapeRoom Display`
- 직원용 앱 세로 화면 고정
- 직원용 앱 화면 꺼짐 방지
- 손님용 Release 빌드에서 debug 서명 강제 설정 제거
- 손님용 Debug 도구는 기존처럼 디버그 가능한 빌드에서만 표시

## 적용 순서
1. Sync Project with Gradle Files
2. Clean Project
3. Rebuild Project
4. `app-manager`와 `app-display` Debug 빌드 실행 확인
5. Release 빌드는 `Generate Signed Bundle / APK`로 별도 서명

## 커밋하지 않는 것이 좋은 파일
- `.idea/`
- `.gradle/`
- `local.properties`
- 모든 `build/` 폴더
- 이전 커밋의 `COMMIT*_APPLY.md`, `APPLY.md`, `MIGRATION.md` 등 작업 안내 파일

현재 `.gitignore`는 위 자동 생성 파일 대부분을 이미 제외합니다.
