# 기존 Git 저장소에 적용하는 방법

이 압축 파일에는 `.git`, `.idea`, `.gradle`, `build`, `local.properties`가 포함되어 있지 않습니다.
기존 직원용 저장소의 Git 이력을 유지하려면 아래 순서로 적용하세요.

1. 현재 직원용 프로젝트를 별도 폴더에 백업합니다.
2. 기존 저장소 루트에서 `.git` 폴더만 남기고 나머지 프로젝트 파일을 정리합니다.
3. 이 압축 파일의 내용을 기존 저장소 루트에 복사합니다.
4. Android Studio에서 저장소 루트를 다시 엽니다.
5. Gradle Sync 후 실행 구성에서 `app-manager` 또는 `app-display`를 선택합니다.
6. 두 모듈을 각각 빌드합니다.

권장 커밋 메시지:

```text
commit012 - Convert manager and display apps to multi-module project
```

## 삭제해도 되는 기존 항목

- 기존 루트의 `app/` 폴더: 새 프로젝트의 `app-manager/`로 대체됩니다.
- 별도 손님용 프로젝트 폴더와 그 안의 `.git`, `.idea`, `.gradle`, `build`, `local.properties`
- 자동 생성 예제 테스트(`ExampleUnitTest`, `ExampleInstrumentedTest`)

## 유지해야 하는 항목

- 기존 직원용 저장소의 `.git/`
- 로컬 환경에서 Android Studio가 다시 만드는 `local.properties`
