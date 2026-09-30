package com.pluxity.aiot.speaker

import com.fasterxml.jackson.databind.ObjectMapper
import com.pluxity.aiot.ldms.LdmsClient
import com.pluxity.aiot.ldms.dto.LdmsEmcallGroupInfo
import com.pluxity.aiot.ldms.dto.LdmsEmcallGroupStatus
import com.pluxity.aiot.ldms.dto.ldmsDeviceStatus
import com.pluxity.aiot.speaker.dto.SpeakerOutput
import com.pluxity.aiot.speaker.dto.SpeakerResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

@Service
class SpeakerService(
    private val ldmsClient: LdmsClient?,
    private val objectMapper: ObjectMapper,
) {
    fun findAll(siteId: Long?): List<SpeakerResponse> {
        val speakers = ldmsClient?.getEmcallGroupList().orEmpty().map { it.toResponse(parseOutput(it)) }
        return if (siteId == null) speakers else speakers.filter { it.site?.id == siteId }
    }

    private fun parseOutput(group: LdmsEmcallGroupInfo): SpeakerOutput? {
        val json = group.emcallGrpStatJson?.takeIf { it.isNotBlank() } ?: return null
        return runCatching { objectMapper.readValue(json, LdmsEmcallGroupStatus::class.java) }
            .onFailure { log.warn { "LDMS 송출그룹 상태 파싱 실패 (seq=${group.emcallGrpSeq}): $json" } }
            .getOrNull()
            ?.let {
                SpeakerOutput(
                    message = it.msg,
                    light = it.light,
                    speaker = it.speaker,
                    speakerTts = it.speakerTts,
                    ttsMessage = it.ttsMsg,
                )
            }
    }
}

private fun LdmsEmcallGroupInfo.toResponse(output: SpeakerOutput?) =
    SpeakerResponse(
        id = emcallGrpSeq,
        name = emcallGrpNm.orEmpty(),
        deviceId = emcallGrpId.orEmpty(),
        location = null,
        latitude = emcallGrpLat,
        longitude = emcallGrpLng,
        status = ldmsDeviceStatus(commStat),
        ttsMessage = emcallTtsMsg,
        output = output,
        site = null,
    )
