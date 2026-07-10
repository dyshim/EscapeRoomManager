# Commit019 적용 방법

프로젝트 루트에 이 ZIP의 내용을 같은 경로로 덮어씁니다.

변경 사항:
- 직원용 위젯: 방별 남은 시간 + 종료 예정 시각 표시
- 손님용 위젯 신규 추가: 선택한 방의 남은 시간 + 종료 예정 시각 + 연결 상태 표시
- 위젯을 누르면 각 앱 실행
- 실행 중 1초마다 위젯 상태 갱신

적용 후:
1. Gradle Sync
2. Clean Project
3. Rebuild Project
4. 기존 홈 화면 위젯 삭제
5. 앱 재설치 후 위젯을 다시 추가

추천 커밋 메시지:
`commit019 - Add remaining time and expected end time to widgets`
