package com.studio.plantspot.presentation.ui.diagnosis

sealed class DiagnosisScreen(val route: String) {
    object Camera : DiagnosisScreen("diagnosis_camera")
    object SpotSelection : DiagnosisScreen("diagnosis_spot")
    object LightMeasurement : DiagnosisScreen("diagnosis_light")
    object Result : DiagnosisScreen("diagnosis_result")
}
