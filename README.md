# Plantspot

카메라로 식물을 촬영해 상태를 진단하고, 진단 이력과 반려 식물을 관리하는 Android 앱입니다.

> 아래 기능들은 `dev/feature/*` 브랜치에서 개발 중이며, 아직 `master`에는 병합되지 않았습니다.

## 주요 기능 (개발 중)

- **AI 진단**: 카메라로 식물을 촬영하고 조도(광량)를 함께 측정해 상태를 진단
- **나의 정원**: 진단 이력과 반려 식물 카드를 관리
- **식물 도감**: 식물 정보, 메모, 통합 캘린더
- **커뮤니티**: 사용자 간 게시판

## 기술 스택

- **언어/UI**: Kotlin, Jetpack Compose (Material 3)
- **DI**: Hilt
- **네비게이션**: Navigation Compose
- **카메라**: CameraX
- **이미지 로딩**: Coil
- **백엔드**: Supabase (Auth, Postgrest, Storage, Functions) via Ktor
- **최소 지원 버전**: minSdk 26 / targetSdk·compileSdk 36

## 시작하기

Android Studio에서 프로젝트를 열거나 CLI로 빌드합니다.

```bash
./gradlew assembleDebug
```

Supabase 연동 기능을 쓰려면 `local.properties`에 다음 값이 필요합니다:

```
SUPABASE_URL=...
SUPABASE_ANON_KEY=...
```

## 프로젝트 구조

```
app/src/main/java/com/studio/plantspot/
  MainActivity.kt      진입점
  ui/theme/             Compose 테마 (Color, Theme, Type)
```

## 브랜치

- `master` — 초기 프로젝트 뼈대
- `dev/feature/ai_diagnosis` — AI 진단 기능
- `dev/feature/my_garden` — 나의 정원(진단 이력) 기능
- `dev/feature/plant_book` — 식물 도감·메모·캘린더 기능
- `dev/feature/community` — 커뮤니티 기능
