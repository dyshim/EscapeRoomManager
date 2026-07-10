# Commit018.1 적용

1. 압축을 푼 뒤 프로젝트 루트에 `app-manager` 폴더를 덮어씁니다.
2. Android Studio에서 Gradle Sync를 실행합니다.
3. Build > Clean Project
4. Build > Rebuild Project

변경 내용:
- 남은 시간 아래에 종료 예정 카드 표시
- 종료 예정 시간은 `오전/오후 h:mm` 형식
- 대기: `시작 후 표시`
- 일시정지: `일시정지 중`
- 종료: `종료됨`
- 남은 시간 색상: 10분 초과 초록, 10분 이하 주황, 5분 이하 빨강
