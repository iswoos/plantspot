# 식물 관리 메모 저장 이슈 해결 구현 계획서

## 개요
현재 `HomeScreen`에서 더미 ID("1", "2" 등)를 사용하고 있어, 상세 화면에서 메모 저장 시 DB 외래키 제약 조건 등으로 인해 실패하는 문제를 해결합니다.

## 제안된 변경 사항

### 1. Data Layer (`data` 패키지)

#### [MODIFY] [PlantRepository.kt](file:///Users/iseung-u/Documents/PlantSpot/app/src/main/java/com/studio/plantspot/data/repository/PlantRepository.kt)
- 특정 식물 ID로 상세 정보를 조회하는 `getPlant(plantId: String)` 함수를 추가합니다.

### 2. Presentation Layer (`ui` 패키지)

#### [NEW] [HomeViewModel.kt](file:///Users/iseung-u/Documents/PlantSpot/app/src/main/java/com/studio/plantspot/ui/screens/home/HomeViewModel.kt)
- 사용자의 모든 식물 목록을 Supabase에서 로드하는 `HomeViewModel`을 생성합니다. (기존 `CareViewModel`은 오늘의 케어 전용으로 유지하거나 통합 검토)

#### [MODIFY] [HomeScreen.kt](file:///Users/iseung-u/Documents/PlantSpot/app/src/main/java/com/studio/plantspot/ui/screens/home/HomeScreen.kt)
- `dummyPlants` 대신 `HomeViewModel`을 통해 가져온 실제 식물 리스트를 표시합니다.

#### [MODIFY] [PlantDetailScreen.kt](file:///Users/iseung-u/Documents/PlantSpot/app/src/main/java/com/studio/plantspot/ui/screens/detail/PlantDetailScreen.kt)
- 전달받은 `plantId`를 사용하여 `PlantRepository.getPlant(plantId)`를 호출, 실제 식물 정보를 화면에 표시합니다.

---

## 상세 구현 계획

### 1. PlantRepository 수정
```kotlin
// PlantRepository object 내부에 추가
suspend fun getPlant(plantId: String): PlantUiModel? {
    return try {
        val dto = db["plantspot_plants"]
            .select(Columns.ALL) {
                filter { eq("id", plantId) }
            }
            .decodeSingle<PlantDto>()

        PlantUiModel(
            id = dto.id,
            aliasName = dto.aliasName,
            species = dto.species,
            iconIndex = dto.characterIndex,
            matchScore = dto.matchScore,
            waterGaugePercent = dto.waterGaugePercent,
            nextWaterDDay = dto.nextWaterDDay,
            lastWateredDate = dto.lastWateredDate,
            memo = dto.memo
        )
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
```

### 2. HomeViewModel 생성
```kotlin
class HomeViewModel : ViewModel() {
    private val _plants = MutableStateFlow<List<PlantUiModel>>(emptyList())
    val plants: StateFlow<List<PlantUiModel>> = _plants.asStateFlow()

    fun loadPlants() {
        viewModelScope.launch {
            val userId = UserPreferences.getUserId()
            _plants.value = PlantRepository.getPlants(userId)
        }
    }
}
```

### 3. HomeScreen & PlantDetailScreen 연동
- `HomeScreen`의 `LaunchedEffect`에서 `homeViewModel.loadPlants()` 호출.
- `PlantDetailScreen`에서 `LaunchedEffect` 또는 별도 ViewModel을 통해 식물 상세 데이터 로드.

---

## 검증 계획

### 자동화 테스트
- `PlantRepository.kt`에 대한 Unit Test를 추가하여 `getPlant` 및 `saveMemo`가 정상 동작하는지 (Mock Supabase 또는 테스트 DB 연동) 확인 가능 여부 검토. (현재 테스트 환경 설정 필요)

### 수동 검증
1. 앱 실행 후 홈 화면에서 실제 DB에 등록된 식물 카드를 클릭. (식물이 없다면 스캐너를 통해 먼저 등록)
2. 상세 화면 진입 후 '관리 메모' 입력란에 텍스트 입력 후 저장 버튼 클릭.
3. 메모 목록에 즉시 추가되는지 확인.
4. 앱 재시작 후 메모가 유지되는지 확인.
