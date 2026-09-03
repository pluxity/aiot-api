package com.pluxity.aiot.mic.dto

import com.pluxity.aiot.mic.Mic
import com.pluxity.aiot.mic.MicEvent
import com.pluxity.aiot.mic.MicStatus
import com.pluxity.aiot.site.dto.SiteResponse
import com.pluxity.aiot.site.dto.toSiteResponse
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "AI 마이크 응답")
data class MicResponse(
    @field:Schema(description = "마이크 아이디", example = "1")
    val id: Long,
    @field:Schema(description = "업체 장비 식별자", example = "d00c8e3364a34439183e3462e773354a1")
    val vendorMicId: String,
    @field:Schema(description = "마이크 명칭", example = "Example Mic (715)")
    val name: String?,
    @field:Schema(description = "장비 호스트", example = "192.168.0.10")
    val host: String?,
    @field:Schema(description = "엣지 장비 식별자")
    val edgeId: String?,
    @field:Schema(description = "장비 상태")
    val status: MicStatus?,
    @field:Schema(description = "위도", example = "37.546344")
    val latitude: Double?,
    @field:Schema(description = "경도", example = "126.944322")
    val longitude: Double?,
    @field:Schema(description = "이벤트 카테고리별 감지 기준값")
    val thresholds: Map<String, MicThreshold>?,
    @field:Schema(description = "소속 현장")
    val site: SiteResponse?,
)

fun Mic.toResponse() =
    MicResponse(
        id = requiredId,
        vendorMicId = vendorMicId,
        name = name,
        host = host,
        edgeId = edgeId,
        status = status,
        latitude = latitude,
        longitude = longitude,
        thresholds = thresholds,
        site = site?.toSiteResponse(),
    )

@Schema(description = "AI 마이크 이벤트 응답")
data class MicEventResponse(
    @field:Schema(description = "이벤트 아이디", example = "1")
    val id: Long,
    @field:Schema(description = "업체 이벤트 식별자", example = "23d6ec74155f4eb187b4e7301b71b5d9")
    val eventId: String,
    @field:Schema(description = "업체 장비 식별자")
    val micId: String,
    @field:Schema(description = "마이크 명칭")
    val micName: String?,
    @field:Schema(description = "이벤트 라벨 식별자", example = "normal_speech_female")
    val labelId: String?,
    @field:Schema(description = "이벤트 라벨명(한글)", example = "대화(여성)")
    val labelNameKo: String?,
    @field:Schema(description = "이벤트 라벨명(영문)", example = "normal speech female")
    val labelNameEn: String?,
    @field:Schema(description = "신뢰도", example = "0.426632")
    val confidence: Double?,
    @field:Schema(description = "위도")
    val latitude: Double?,
    @field:Schema(description = "경도")
    val longitude: Double?,
    @field:Schema(description = "소음 측정 원본 배열(dB)")
    val noises: List<Double>?,
    @field:Schema(description = "최대 소음(dB)", example = "70.66")
    val maxNoise: Double?,
    @field:Schema(description = "평균 소음(dB)", example = "64.39")
    val avgNoise: Double?,
    @field:Schema(description = "이벤트 발생 시각")
    val occurredAt: LocalDateTime,
)

fun MicEvent.toResponse() =
    MicEventResponse(
        id = requiredId,
        eventId = eventId,
        micId = micId,
        micName = micName,
        labelId = labelId,
        labelNameKo = labelNameKo,
        labelNameEn = labelNameEn,
        confidence = confidence,
        latitude = latitude,
        longitude = longitude,
        noises = noises,
        maxNoise = maxNoise,
        avgNoise = avgNoise,
        occurredAt = occurredAt,
    )
