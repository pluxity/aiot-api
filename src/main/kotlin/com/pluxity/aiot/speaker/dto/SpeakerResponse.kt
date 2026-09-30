package com.pluxity.aiot.speaker.dto

import com.pluxity.aiot.global.constant.DeviceStatus
import com.pluxity.aiot.site.dto.SiteResponse
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "스피커 응답")
data class SpeakerResponse(
    @field:Schema(description = "스피커 아이디", example = "1")
    val id: Long,
    @field:Schema(description = "스피커 명칭", example = "정문 스피커")
    val name: String,
    @field:Schema(description = "업체 장비 식별자. 송출 시 업체 API 로 전달합니다", example = "SPK-0001")
    val deviceId: String,
    @field:Schema(description = "설치 위치", example = "정문 매표소 상단")
    val location: String?,
    @field:Schema(description = "위도", example = "37.4428")
    val latitude: Double?,
    @field:Schema(description = "경도", example = "127.174")
    val longitude: Double?,
    @field:Schema(description = "장치 상태")
    val status: DeviceStatus,
    @field:Schema(description = "기본 TTS 메시지", example = "스피커송출 메세지입니다.")
    val ttsMessage: String?,
    @field:Schema(description = "최근 출력 상태. 송출 이력이 없으면 null")
    val output: SpeakerOutput?,
    @field:Schema(description = "소속 현장")
    val site: SiteResponse?,
)

@Schema(description = "스피커 출력 상태 (On/Off)")
data class SpeakerOutput(
    @field:Schema(description = "전광판", example = "Off")
    val message: String?,
    @field:Schema(description = "경광등", example = "Off")
    val light: String?,
    @field:Schema(description = "저장된 음성 송출", example = "Off")
    val speaker: String?,
    @field:Schema(description = "TTS 음원 송출", example = "Off")
    val speakerTts: String?,
    @field:Schema(description = "TTS 메시지", example = "IDLE")
    val ttsMessage: String?,
)
