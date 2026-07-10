# Commit018 적용 안내

## 변경 파일
- `app-manager/src/main/java/com/example/escaperoomtimer/manager/TimerManager.kt`
- `app-manager/src/main/java/com/example/escaperoomtimer/ui/timer/TimerScreen.kt`

## 기능
- 직원용 방 상세 화면을 Galaxy A7 2018 세로 화면에 맞게 스크롤형으로 개선
- +10분, +5분, +1분, +30초, +10초
- -10분, -5분, -1분, -30초, -10초
- 분/초 직접 입력
- 시간 변경 후 5초간 실행 취소 표시
- 모든 변경은 기존 TCP 상태 전송을 통해 손님용 앱에 반영

## 적용 후
1. Gradle Sync
2. Clean Project
3. Rebuild Project
4. app-manager와 app-display 실행 테스트
