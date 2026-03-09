package com.studio.plantspot.presentation.ui.calendar

import com.studio.plantspot.domain.entity.Memo
import com.studio.plantspot.domain.entity.PlantMemo
import java.time.LocalDate

/**
 * 통합 캘린더 및 타임라인에서 다루는 이벤트들의 공용 래퍼 클래스입니다.
 */
sealed class IntegratedEvent {
    // 공통 필드: 정렬을 위한 날짜
    abstract val date: LocalDate

    /**
     * 식물 물주기 이벤트
     */
    data class Watering(
        override val date: LocalDate,
        val plantId: String,
        val plantNickname: String
    ) : IntegratedEvent()

    /**
     * 식물 전용 메모 이벤트
     */
    data class PlantSpecificMemo(
        override val date: LocalDate,
        val plantId: String,
        val plantNickname: String,
        val memo: PlantMemo
    ) : IntegratedEvent()

    /**
     * 다이어리(일반) 메모 이벤트
     */
    data class GeneralMemo(
        override val date: LocalDate,
        val memo: Memo
    ) : IntegratedEvent()

    /**
     * 식물 진단 내역 이벤트
     */
    data class PlantDiagnosis(
        override val date: LocalDate,
        val plantId: String,
        val plantNickname: String,
        val history: com.studio.plantspot.domain.entity.PlantDiagnosisHistory
    ) : IntegratedEvent()
}
