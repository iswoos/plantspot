# Gemini API 오류 해결 계획 (plan.md)

## 1. 개요
- Supabase Edge Function에서 발생한 `Unknown name "response_mime_type"` 오류를 해결하기 위해 Gemini API 호출 엔드포인트를 `v1beta`로 업그레이드합니다.

## 2. 변경 사항

### [Supabase Edge Function] - `plantspotDiagnosis`
- **목적**: 앱의 `DiagnosisResult.kt` 엔티티 구조와 AI 응답 JSON을 100% 일치시켜 파싱 오류를 해결합니다.
- **수정 사항**:
    - `RECOMMEND` 모드: `spaceAnalysis`, `recommendedPlants`, `measuredLux` 키 사용.
    - 일반 진단 모드: `plantName`, `healthStatus`, `analysis`, `measuredLux`, `matchScore`, `solution`, `careTips` 키 사용.
    - 모든 텍스트 필드는 **Plain String**으로 반환하도록 강제 (객체 중첩 금지).
- **최종 해결 로직 (2026년 표준)**:
    1. **모델 업그레이드**: 2026년 현재 최신 표준인 **`gemini-2.5-flash`**로 모델을 변경합니다.
    2. **모델 리스트 진단**: 실행 시 `genAI.listModels()`를 호출해 현재 사용 가능한 모든 모델 목록을 로그로 남깁니다. 이를 통해 모델명 불일치를 즉각 확인 가능합니다.
    3. **정형 파싱 유지**: SDK 응답에서 JSON만 골라내는 견고한 파싱 로직을 유지합니다.

#### [MODIFY] `plantspotDiagnosis/index.ts`
```typescript
// [2026년형 최신 표준 코드]
const model = genAI.getGenerativeModel({ model: "gemini-2.5-flash" });

// 가용 모델 목록 로그 출력 (진단용)
const models = await genAI.listModels();
console.log("Available Models:", models.map(m => m.name));
```

### [Android App] - `SupabaseModule.kt`
- **목적**: AI 분석 시간을 충분히 확보하기 위해 타임아웃을 연장합니다.
- **수정 사항**:
    - `Functions` 플러그인 설정에 `httpTimeout = 60.seconds` 추가.

## 3. 검증 계획

### 자동 테스트
- [x] `./gradlew help`를 통한 빌드 구성 검증 (플러그인/의존성 설정 확인)

### 수동 테스트
1. **타임아웃 확인**: 앱에서 진단 시작 후 10초 이상 소요되어도 에러 없이 결과 화면으로 진입하는지 확인.
2. **시간 인식 확인**: 새벽 2시에 테스트 시 AI 응답에 "새벽 2시" 또는 "오전 2시"와 같이 정확한 시간 맥락이 포함되는지 확인.
1. **Edge Function 배포**: 수정된 코드를 Supabase에 배포합니다.
   - 명령어 예시: `supabase functions deploy plantspotDiagnosis`
2. **앱에서 진단 요청**: Android 앱의 '공간진단' 또는 '식물진단' 페이지에서 측정을 수행하고 진단 결과를 확인합니다.
3. **Logcat 확인**: 앱에서 진단 결과가 정상적으로 출력되는지, 또는 에러 발생 시 로그를 확인합니다.
4. **Supabase 로그 확인**: Supabase 대시보드의 Edge Function 로그에서 `Gemini API Error`가 더 이상 발생하지 않는지 확인합니다.

### 주의 사항
- `v1beta` 엔드포인트는 개발 중인 기능이 포함되어 있으므로, 추후 구글의 API 업데이트에 따라 사양이 변경될 수 있습니다. 하지만 현재 Gemini 1.5의 JSON 모드 활용을 위해서는 가장 권장되는 방식입니다.
