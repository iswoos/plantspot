# Research Report: 기존 식물 진단 시 이미지 업데이트 분석

## 1. 현상 및 요구사항
- 현재 `DiagnosisViewModel`은 기존 식물을 진단할 때 `match_score`만 업데이트함.
- 사용자는 진단 시 촬영된 최신 이미지를 Storage에 저장하고, 이를 식물의 대표 이미지로 업데이트하기를 원함.

## 2. 분석 결과
- **Domain Layer**: `PlantRepository` 인터페이스의 `updateDiagnosisResult` 메서드에 `imageUrl` 파라미터가 누락되어 있음.
- **Data Layer**: `PlantRepositoryImpl`에서 DB 업데이트 시 `imageUrl` 필드를 포함하도록 수정이 필요함.
- **Presentation Layer**: `DiagnosisViewModel.startDiagnosis` 함수에서 진단 성공 후 이미지를 업로드하고, 얻어진 URL을 Repository에 전달하는 로직이 추가되어야 함.

## 3. 해결 방안
- `PlantRepository` 인터페이스 및 구현체 수정.
- `DiagnosisViewModel`에서 `DIAGNOSE` 모드(기존 식물 진단)일 때 이미지 업로드 프로세스 추가.
