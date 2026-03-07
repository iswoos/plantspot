### 13.3 JSON 파싱 오류 (Illegal input - Type Mismatch)
- **현상**: `Expected beginning of the string, but got { at path: $.analysis`
- **원인**: 
    - 앱의 `DiagnosisResult.kt`에서 `analysis` 필드는 `String?` 임.
    - AI는 `analysis`를 `{ "title": "...", "details": "..." }` 형태의 **JSON 객체**로 반환함.
    - JSON 라이브러리가 문자열 자리에 객체가 오자 파싱 실패.
- **해결**: AI 프롬프트에 반환할 JSON의 스키마(키 이름 및 데이터 타입)를 매우 구체적으로 명시함.

## 1. 오류 현상 분석
- **오류 메시지**: `Invalid JSON payload received. Unknown name "response_mime_type" at 'generation_config': Cannot find field.`
- **발생 위치**: Supabase Edge Function (`plantspotDiagnosis`) 내 Gemini API 호출부.
- **원인 추정**: 
    - Gemini API의 `v1` 정식 버전 엔드포인트에서는 `generationConfig` 내의 `response_mime_type` 필드를 인식하지 못함.
    - JSON 모드(`response_mime_type: "application/json"`)는 주로 `v1beta` 엔드포인트에서 지원되거나, 최신 `v1` 버전에서 특정 모델(Gemini 1.5 Flash/Pro)에 대해 지원됨.
    - 현재 Edge Function은 `v1` 엔드포인트를 사용 중임.

## 2. Gemini API 사양 확인
- **Endpoint**: `https://generativelanguage.googleapis.com/v1/models/gemini-1.5-flash:generateContent`
- **JSON Mode 지원 여부**: 
    - Google 공식 문서에 따르면, Gemini 1.5 Flash 및 Pro 모델은 JSON 출력 모드를 지원함.
    - 하지만 REST API 호출 시 `v1` 엔드포인트에서는 이 필드명이 `responseMimeType`(camelCase)으로 요구되거나, 아예 `v1beta`를 사용해야 할 수 있음.
    - 에러 로그 상 `Unknown name "response_mime_type"`이라고 뜨는 것으로 보아, 해당 필드 자체를 해당 엔드포인트(`v1`)가 이해하지 못하고 있음.

## 3. 해결 방안 검토
### 방안 A: API 버전을 v1beta로 변경
- 엔드포인트 URL을 `v1`에서 `v1beta`로 변경.
- `v1beta`는 `response_mime_type` 필드를 확실히 지원함.

## 4. 2차 오류 분석 (404 Not Found)
- **오류 메시지**: `models/gemini-1.5-flash is not found for API version v1beta`
- **원인**: 특정 지역이나 시점에 따라 `v1beta`에서 `gemini-1.5-flash` 모델 식별자가 다를 수 있거나 지원되지 않을 수 있음.
- **새로운 발견**: Gemini REST API 문서 확인 결과, `v1` 엔드포인트에서도 `generationConfig` 내의 필드명을 **`responseMimeType` (camelCase)**으로 작성하면 정상적으로 인식됨을 확인. (기존 코드는 `response_mime_type`인 snake_case를 사용했음)

## 6. 근본 원인 정밀 분석 (RCA)
- **400 오류 (Unknown name)**: Gemini `v1` 정식 엔드포인트는 일부 리전이나 특정 모델 환경에서 `generationConfig` 내의 JSON 모드 설정(`response_mime_type`)을 아직 지원하지 않음. snake_case와 camelCase 모두 실패하는 것으로 보아 해당 필드 자체가 차단되어 있음.
- **404 오류 (Not Found)**: `v1beta` 엔드포인트에서 `gemini-1.5-flash` 모델명을 찾지 못하는 것은 해당 리전의 서비스 전파 지연이나 특정 모델 ID 사용 규칙(`gemini-1.5-flash-latest` 등) 때문일 가능성이 높음.

## 7. 최선의 전략 (똑바로 처리하기)
1. **안정성 우선**: `v1beta`의 불안정함(404)을 피하기 위해 **`v1`** 엔드포인트 사용.
2. **오류 원인 제거**: `v1`에서 문제를 일으키는 `responseMimeType` 필드를 과감히 제거 (이 기능 없이도 프롬프트만으로 JSON 추출이 충분히 가능함).
3. **프롬프트 강화**: AI가 다른 사족 없이 오직 JSON만 출력하도록 지시를 극대화.
4. **파싱 신뢰도 유지**: 이미 코드에 포함된 `jsonMatch` (정규식 기반 JSON 추출) 로직을 활용하여 AI가 앞뒤에 설명을 붙이더라도 완벽하게 데이터만 골라내도록 함.
