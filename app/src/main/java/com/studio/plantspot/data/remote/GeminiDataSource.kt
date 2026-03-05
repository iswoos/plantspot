package com.studio.plantspot.data.remote

import android.graphics.Bitmap
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import com.studio.plantspot.data.repository.ConfigRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeminiDataSource(
    private val configRepository: ConfigRepository
) {

    private suspend fun getModel(): GenerativeModel? {
        val apiKey = configRepository.getGeminiApiKey() ?: return null
        return GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = apiKey,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
            }
        )
    }

    /**
     * 공간 진단 (입양 전) 호출
     */
    suspend fun getSpaceAnalysis(
        image: Bitmap,
        lux: Int,
        currentTime: String
    ): String = withContext(Dispatchers.IO) {
        val model = getModel() ?: return@withContext "{\"error\": \"API Key 가 준비되지 않았습니다.\"}"

        val prompt = """
            너는 20년 경력의 식물 전문가이자 인테리어 디자이너야.
            첨부된 사진은 사용자가 식물을 새로 들이고자 하는 공간이야.
            
            [데이터]
            - 현재 시간: $currentTime
            - 측정된 조도: $lux LUX
            
            이 데이터를 바탕으로 공간을 분석하고 분위기에 어울리는 식물을 최소 3개 추천해줘.
            반드시 아래 JSON 규격을 지켜서 응답해줘. 다른 텍스트는 포함하지 마.
            
            [응답 규격]
            {
              "space_analysis": "공간의 채광 상태, 인테리어 톤, 가구 배치 등 특징 분석 설명",
              "recommended_plants": [
                {
                  "name": "식물 명칭",
                  "reason": "추천 근거 (조도/인테리어 조화 등)",
                  "suitability_score": 적합도 점수 (0-100)
                }
              ]
            }
        """.trimIndent()

        try {
            val response = model.generateContent(
                content {
                    image(image)
                    text(prompt)
                }
            )
            response.text ?: ""
        } catch (e: Exception) {
            "{\"error\": \"${e.message}\"}"
        }
    }

    /**
     * 식물 진단 (입양 후) 호출
     */
    suspend fun getPlantDiagnosis(
        image: Bitmap,
        lux: Int,
        currentTime: String
    ): String = withContext(Dispatchers.IO) {
        val model = getModel() ?: return@withContext "{\"error\": \"API Key 가 준비되지 않았습니다.\"}"

        val prompt = """
            너는 20년 경력의 식물 의사야.
            첨부된 사진 속 식물의 상태를 진단해줘.
            
            [데이터]
            - 현재 시간: $currentTime
            - 측정된 조도: $lux LUX
            
            이 데이터를 바탕으로 식물의 종류와 건강 상태를 분석하고 해결책을 제시해줘.
            반드시 아래 JSON 규격을 지켜서 응답해줘. 다른 텍스트는 포함하지 마.
            
            [응답 규격]
            {
              "plant_name": "식물 이름",
              "health_status": "건강 등급 (normal, warning, danger 중 하나)",
              "analysis": "상태 분석 보고서 (이미지+조도+시간 결합 분석)",
              "measured_lux": $lux,
              "match_score": 환경 일치 점수 (0-100),
              "solution": "구체적인 환경 개선 조치 및 해결법",
              "care_tips": {
                "humidity": "습도 관리 정보",
                "soil": "토양 관리 정보"
              }
            }
        """.trimIndent()

        try {
            val response = model.generateContent(
                content {
                    image(image)
                    text(prompt)
                }
            )
            response.text ?: ""
        } catch (e: Exception) {
            "{\"error\": \"${e.message}\"}"
        }
    }
}
