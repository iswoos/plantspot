# PlantSpot 사용자 환경(UI) 구현 계획서

## 1. 아키텍처 원칙 및 기존 Amen 프로젝트 재활용 방안
*   **UI 패러다임**: 100% Jetpack Compose를 활용한 선언형 UI 구현.
*   **디자인 시스템 및 테마**: Material Design 3 컬러 팔레트를 자연주의(Nature-inspired) 컨셉으로 재구성. Sage Green(`Primary`), Warm Earth(`Secondary/Surface`) 색상을 `Color.kt`에 정의하여 적용.
    *   **공통 적용 (Amen 참고)**: `strings.xml` 기반 글로벌 언어팩 준비, `local.properties` 및 `.gitignore`를 통한 API 보안, Color/Typography 테마 리소스 분리 기법 차용.

## 2. 화면 (Screen) 및 컴포넌트 Compose 설계

### 2.1 메인 네비게이션 뼈대 (App Scaffold)
*   **요소**: `BottomNavigationBar` + `NavHost`.
*   **탭 구성**: Home (나의 정원), Encyclopedia (식물 백과), Calendar (통합 달력), Lounge (커뮤니티).
*   **특별 버튼**: 하단 탭 중앙에 `FloatingActionButton`을 크게 배치하여 '스마트 스캐너' 모달(또는 전체 화면) 호출.

### 2.2 메인 탭: 나의 정원 (MyForestScreen)
*   **`GreetingHeader`**: 유저 정보("민수님의 정원") 및 다정한 인사말 노출 영역.
*   **`QuickCareRow` (퀵 케어 요소)**: 당일 일정이 있는 식물을 최우선 노출하고, 우측에 `IconButton(Check)`를 두어 원클릭 물주기 완료를 트리거.
*   **`PlantDashBoard` (LazyVerticalGrid)**:
    *   식물 카드 컨테이너(`PlantCard`) 컴포저블.
    *   **내부 구성**: 썸네일 이미지, 상단 귀퉁이에 매치 스코어 기반 다마고치 캐릭터(아이콘), 하단에 이름/애칭 텍스트와 [수분 게이지 Progress Indicator].
    *   **캐릭터 편집 트리거**: 카드의 상단 캐릭터나 애칭 영역을 길게 누르거나 편집 버튼을 눌러 `CharacterSelectionSheet` 모달 띄우기 가능.
*   **`CharacterSelectionSheet`**: 상업적 무료 3D 에셋들 중 원하는 외형을 고르는 하단 모달.
*   **`ShareBottomSheet`**: '공유하기' 클릭 시 나타나는 BottomSheet로 갤러리/카메라 선택 유도.

### 2.3 스마트 스캐너 (ScannerScreen)
*   **`CameraPreviewSection`**: CameraX를 이용한 실시간 카메라 스트리밍 뷰.
*   **`ModeToggleSwitch`**: [공간 분석(입양 전)] / [진단(입양 후)] 토글 버튼. (선택에 따라 문구 및 가이드 박스 UI 변경)
*   **`LightMeterOverlay`**:
    *   조도 센서(Sensor.TYPE_LIGHT)에서 받은 값을 바인딩하여 실시간 Lux 수치를 보여주는 중앙 링 뷰.
*   **`InstructionAnimation`**: "식물 자리에 눕혀주세요" 모션 그래픽 팝업 컨테이너 (평소엔 축소, 측정 시 확대).

### 2.4 상세 관리, 도감, 캘린더
*   **통합 식물 상세 뷰 (`PlantDetailScreen`)**:
    *   스크롤 가능한 뷰페이저. 사진과 주요 지표(매치 스코어, 물 주기) 상단 고정, 하단에 다이어리 및 관리 일지 텍스트 폼 구성.
*   **도감 리스트 (`EncyclopediaScreen`)**:
    *   상단 검색바 및 가로 스크롤 가능한 키워드 칩 그룹(반양지, 음지, 초보자용 등).
*   **물 주기 캘린더 (`CareCalendarScreen`)**:
    *   달력 컴포저블 블록을 구성하고, 내부 각 일자 칸에 스케줄 유무를 점(Dot)이나 물방울 아이콘으로 매핑.

### 2.5 프로필 및 정보 수정 (Modals & Sheets)
*   **`UserProfileEditSheet`**: 우측 상단이나 라운지 탭에서 유저 닉네임("민수")을 변경하는 바텀 시트.
*   **`PlantAliasEditSheet`**: (2.2에 언급된 CharacterSelectionSheet 확장본) 캐릭터 외형 변경 및 식물의 유저 설정 애칭을 즉시 수정하는 입력 폼 제공.

## 3. 구현 프로세스 및 투두리스트 (ToDo)

- [ ] **Phase 1: 기반 설정 및 디자인 시스템 완성**
  - 기존 프로젝트(Amen) 이식(보안 설정, 의존성 정리)
  - Color, Typography, Shape 규격 세팅
  - `strings.xml` 초기 다국어 키값 분리

- [ ] **Phase 2: 내비게이션 및 목업(Mock) UI 구축**
  - `BottomNavigation` 프레임 구축
  - `MyForestScreen` 및 `PlantCard` 더미 데이터 기반 UI 마크업
  - 다마고치 감성 확인을 위한 스코어별/시간대별 UI 상태 변화 검증

- [ ] **Phase 3: 핵심 모듈 (카메라 & 스캐너) UI 구축**
  - CameraX 프리뷰 연동 및 투명 오버레이 레이아웃 구성
  - 조도 측정 다이얼 UI 컴포넌트 설계 (상태값 주입 준비)

- [ ] **Phase 4: 자연주의 테마 적용 및 수정 기능 UI 구축**
  - `Color.kt` 및 `Theme.kt`에 식물 앱 전용 컬러(Sage Green 등) 적용
  - 유저 닉네임 변경 폼(`UserProfileEditSheet`) 마크업
  - 식물 애칭 및 캐릭터 설정 폼(`PlantAliasEditSheet`) 마크업

## 4. 리뷰 요청
사용자님, 디자인 시스템(자연주의 컬러 팔레트) 변경 로직과 기존에 구현하지 않았던 이름 변경(유저명, 식물 애칭) 화면 구성을 추가 반영했습니다. 확인 후 피드백 & 승인 부탁드립니다!
