# Commit031 적용 안내

변경 내용:
- 손님용 앱의 관리자 진입 제스처를 방 이름 5회 터치에서 10회 터치로 변경
- 입력 제한 시간을 3초에서 5초로 변경
- 손님이 우연히 관리자 PIN 화면을 여는 가능성 감소

적용 방법:
1. ZIP 압축을 풉니다.
2. `app-display/src/main/java/com/example/escaperoomdisplay/MainActivity.kt`를 프로젝트의 동일 경로에 덮어씁니다.
3. Gradle Sync 후 Clean Project, Rebuild Project를 실행합니다.
4. 손님용 앱에서 방 이름을 5초 안에 10회 터치해 관리자 PIN 화면이 나타나는지 확인합니다.

추천 커밋 메시지:
`commit031 - Require ten taps for display admin access`
