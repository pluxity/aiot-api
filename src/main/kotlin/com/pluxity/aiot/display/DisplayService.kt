package com.pluxity.aiot.display

import com.fasterxml.jackson.databind.ObjectMapper
import com.pluxity.aiot.broadcast.DeviceStatus
import com.pluxity.aiot.display.dto.DisplayBroadcastGroupOutput
import com.pluxity.aiot.display.dto.DisplayBroadcastGroupResponse
import com.pluxity.aiot.display.dto.DisplayResponse
import com.pluxity.aiot.ldms.LdmsClient
import com.pluxity.aiot.ldms.dto.LdmsEbrdInfo
import com.pluxity.aiot.ldms.dto.LdmsEmcallGroupInfo
import com.pluxity.aiot.ldms.dto.LdmsEmcallGroupStatus
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

@Service
class DisplayService(
    private val ldmsClient: LdmsClient?,
    private val objectMapper: ObjectMapper,
) {
    fun findAll(siteId: Long?): List<DisplayResponse> {
        val displays = ldmsClient?.getEbrdList().orEmpty().map { it.toResponse() }
        return if (siteId == null) displays else displays.filter { it.site?.id == siteId }
    }

    fun findBroadcastGroups(): List<DisplayBroadcastGroupResponse> =
        ldmsClient?.getEmcallGroupList().orEmpty().map { it.toResponse(parseOutput(it)) }

    private fun parseOutput(group: LdmsEmcallGroupInfo): DisplayBroadcastGroupOutput? {
        val json = group.emcallGrpStatJson?.takeIf { it.isNotBlank() } ?: return null
        return runCatching { objectMapper.readValue(json, LdmsEmcallGroupStatus::class.java) }
            .onFailure { log.warn { "LDMS 송출그룹 상태 파싱 실패 (seq=${group.emcallGrpSeq}): $json" } }
            .getOrNull()
            ?.let {
                DisplayBroadcastGroupOutput(
                    message = it.msg,
                    light = it.light,
                    speaker = it.speaker,
                    speakerTts = it.speakerTts,
                    ttsMessage = it.ttsMsg,
                )
            }
    }
}

private fun deviceStatus(commStat: String?) =
    when (commStat) {
        "Ok" -> DeviceStatus.NORMAL
        "Err" -> DeviceStatus.OFFLINE
        else -> DeviceStatus.UNKNOWN
    }

private fun LdmsEbrdInfo.toResponse() =
    DisplayResponse(
        id = ebrdSeq,
        name = ebrdNm.orEmpty(),
        deviceId = ebrdId.orEmpty(),
        location = ebrdDesc,
        latitude = ebrdLat,
        longitude = ebrdLng,
        status = deviceStatus(commStat),
        site = null,
    )

private fun LdmsEmcallGroupInfo.toResponse(output: DisplayBroadcastGroupOutput?) =
    DisplayBroadcastGroupResponse(
        id = emcallGrpSeq,
        name = emcallGrpNm.orEmpty(),
        deviceId = emcallGrpId.orEmpty(),
        latitude = emcallGrpLat,
        longitude = emcallGrpLng,
        status = deviceStatus(commStat),
        ttsMessage = emcallTtsMsg,
        output = output,
    )
