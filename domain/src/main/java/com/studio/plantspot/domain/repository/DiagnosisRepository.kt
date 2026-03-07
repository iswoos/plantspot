package com.studio.plantspot.domain.repository

import com.studio.plantspot.domain.entity.DiagnosisResult

interface DiagnosisRepository {
    suspend fun getDiagnosis(
        image: ByteArray,
        lux: Float,
        hour: Int,
        date: String,
        mode: String
    ): DiagnosisResult
}
