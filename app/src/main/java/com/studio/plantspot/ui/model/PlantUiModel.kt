package com.studio.plantspot.ui.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EnergySavingsLeaf
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.Nature
import androidx.compose.material.icons.filled.NaturePeople
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Yard
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Serializable

/**
 * 식물 다마고치 캐릭터 데이터
 * Material Icons Extended 기반 — Apache 2.0 라이선스 (상업적 사용 무료)
 */
data class CharacterData(
    val index: Int,
    val name: String,        // 캐릭터 이름 (사용자에게 표시)
    val description: String, // 간단한 설명
    val icon: ImageVector
)

object PlantCharacters {
    val all = listOf(
        CharacterData(0, "새싹이",  "막 태어난 귀여운 새싹",      Icons.Filled.Grass),
        CharacterData(1, "꽃봄이",  "화사하게 피어오른 꽃",        Icons.Filled.LocalFlorist),
        CharacterData(2, "초록이",  "싱그러운 초록 잎사귀",        Icons.Filled.EnergySavingsLeaf),
        CharacterData(3, "잎파리",  "청량한 나뭇잎 느낌",          Icons.Filled.Nature),
        CharacterData(4, "숲속이",  "숲의 정령 같은 캐릭터",       Icons.Filled.Park),
        CharacterData(5, "정원이",  "넓은 정원을 꾸미는 가드너",   Icons.Filled.Yard),
        CharacterData(6, "물방울",  "촉촉하게 물을 좋아하는 식물", Icons.Filled.WaterDrop),
        CharacterData(7, "햇살이",  "햇살을 듬뿍 받은 활기찬 식물",Icons.Filled.WbSunny),
        CharacterData(8, "둥굴레",  "둥글고 평화로운 자연",        Icons.Filled.Spa),
        CharacterData(9, "자연인",  "자연과 하나된 존재",          Icons.Filled.NaturePeople)
    )

    fun getByIndex(index: Int): CharacterData = all.getOrElse(index) { all[0] }
}

@Serializable
data class PlantUiModel(
    val id: String,
    val aliasName: String,
    val species: String,
    val iconIndex: Int,
    val matchScore: Int,
    val waterGaugePercent: Float,
    val nextWaterDDay: Int,
    val lastWateredDate: String = "2024.03.01",
    val memo: String = ""
) {
    val characterData: CharacterData
        get() = PlantCharacters.getByIndex(iconIndex)

    val characterIcon: ImageVector
        get() = characterData.icon
}
