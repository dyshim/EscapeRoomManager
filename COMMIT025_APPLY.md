# Commit025 적용 방법

1. ZIP을 프로젝트 루트에 덮어씁니다.
2. Android Studio에서 **Sync Project with Gradle Files**를 실행합니다.
3. **Build > Clean Project** 후 **Build > Rebuild Project**를 실행합니다.
4. `app-manager`, `app-display`를 각각 실행해 글자 크기와 버튼 배치를 확인합니다.

## 변경 내용

- 직원용 앱을 고대비 검정 배경으로 통일
- 직원 홈 화면 제목, 현재 시각, 연결 주소 확대
- 방 카드 높이 및 방 이름/남은 시간 확대
- 직원 상세 타이머를 78sp로 확대
- 종료 예정 시각과 상태 글자 대비 개선
- 직원 제어 버튼 높이 확대
- 손님용 방 이름, START, 남은 시간, 안내 문구 확대
- 손님용 보조 글자색을 밝게 조정
- 동적 색상 대신 고정된 어두운 색상 체계 사용
- A7/S7/Q6 세로 화면에서 가독성 우선

## 권장 커밋 메시지

`commit025 - Improve mobile readability and high contrast UI`
