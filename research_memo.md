# 식물 관리 메모 저장 실패 원인 분석 보고서

## 1. 개요
사용자가 식물 상세 화면에서 관리 메모를 저장하려 할 때, 저장이 되지 않는 현상이 발생함. 이를 분석한 결과, 화면 간 데이터 전달 및 DB 연동 과정에서 더미(Dummy) 데이터를 사용하고 있는 것이 주요 원인으로 파악됨.

## 2. 상세 분석 결과

### 2.1. 홈 화면의 더미 데이터 사용 (HomeScreen.kt)
`HomeScreen.kt`에서 식물 목록을 보여줄 때, 실제 DB에서 가져온 데이터가 아닌 코드에 고정된 `dummyPlants`를 사용하고 있음.
- **코드 위치**: `app/src/main/java/com/studio/plantspot/ui/screens/home/HomeScreen.kt`
- **문제 지점**:
  ```kotlin
  val dummyPlants = listOf(
      PlantUiModel("1", "초록이", "드라세나 마르기나타", 0, 95, 0.6f, 2),
      PlantUiModel("2", "뾰족이", "몬스테라", 1, 88, 0.3f, 1),
      PlantUiModel("3", "둥글이", "고무나무", 2, 92, 0.8f, 5)
  )
  ```
  위와 같이 ID가 "1", "2", "3"으로 설정되어 있으며, 이 ID들이 상세 화면으로 전달됨.

### 2.2. 상세 화면의 저장 로직 (PlantDetailScreen.kt & MemoViewModel.kt)
상세 화면에서는 전달받은 `plantId`를 사용하여 메모를 저장함.
- **문제 지점**: `MemoViewModel.saveMemo(plantId, content)` 호출 시 `plantId`가 "1", "2", "3"과 같은 더미 값임.
- **서버 측 영향**: Supabase의 `plantspot_memos` 테이블은 `plant_id` 외래키(Foreign Key) 제약 조건이 있을 가능성이 높음. 실제 `plantspot_plants` 테이블에 존재하지 않는 ID("1" 등)로 저장을 시도하면 DB 수준에서 오류가 발생하여 저장이 실패함.

### 2.3. 상세 데이터 로드 부재
`PlantDetailScreen.kt`에서도 식물 상세 정보를 실제 DB에서 가져오지 않고 더미 데이터를 출력하고 있음.
- **코드 위치**: `app/src/main/java/com/studio/plantspot/ui/screens/detail/PlantDetailScreen.kt`
- **내용**:
  ```kotlin
  var plant by remember {
      mutableStateOf(PlantUiModel(plantId, "초록이", "드라세나 마르기나타", 0, 95, 0.6f, 2))
  }
  ```

## 3. 결론 및 해결 방향
현재 시스템은 UI 껍데기만 구현된 상태에서 실제 DB 저장 로직(MemoRepository)이 동작하려다 보니 ID 불일치로 실패하고 있습니다.
이를 해결하기 위해 다음의 조치가 필요합니다:
1. **HomeScreen**: `dummyPlants` 대신 `PlantRepository.getPlants(userId)`를 호출하여 실제 DB에 등록된 식물을 표시하도록 수정.
2. **PlantDetailScreen**: 전달받은 `plantId`를 기반으로 `PlantRepository`를 통해 상세 정보를 조회하도록 수정.
3. **MemoViewModel**: 실제 존재하는 `plantId`가 전달되므로 저장 로직이 정상 동작하게 됨.

위 사항을 바탕으로 구현 계획서를 작성하겠습니다.
