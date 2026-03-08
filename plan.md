# Implementation Plan: 기존 식물 진단 시 이미지 업데이트

## 1. 개요
기존 식물의 건강 상태를 진단할 때마다 최신 사진으로 정원 리스트의 이미지가 갱신되도록 기능을 확장합니다.

## 2. 제안된 변경 사항

### [Domain Layer]
#### [MODIFY] [PlantRepository.kt](file:///Users/iseung-u/Documents/Plantspot/domain/src/main/java/com/studio/plantspot/domain/repository/PlantRepository.kt)
- `updateDiagnosisResult(plantId: String, score: Int, imageUrl: String?)`로 시그니처 변경.

### [Data Layer]
#### [MODIFY] [PlantRepositoryImpl.kt](file:///Users/iseung-u/Documents/Plantspot/data/src/main/java/com/studio/plantspot/data/repository/PlantRepositoryImpl.kt)
- `imageUrl`이 존재할 경우 `plantspot_user_plants` 테이블의 `image_url` 필드도 함께 업데이트하도록 수정.

### [Presentation Layer]
#### [MODIFY] [DiagnosisViewModel.kt](file:///Users/iseung-u/Documents/Plantspot/presentation/src/main/java/com/studio/plantspot/presentation/ui/diagnosis/DiagnosisViewModel.kt)
- `startDiagnosis`에서 `_selectedPlantId`가 있을 경우:
    1. 사진(근접 사진 우선)을 업로드.
    2. 생성된 URL과 진단 점수를 함께 업데이트 호출.

## 3. 검증 계획
- **수동 검증**: 기존 입양된 식물을 선택하여 진단 수행 -> 진단 완료 후 대시보드로 돌아왔을 때 해당 식물의 사진이 최신 사진으로 바뀌어 있는지 확인.
