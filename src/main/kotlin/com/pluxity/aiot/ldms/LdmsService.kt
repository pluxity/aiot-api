package com.pluxity.aiot.ldms

import com.fasterxml.jackson.databind.ObjectMapper
import com.pluxity.aiot.ldms.dto.LdmsDisplayResponse
import com.pluxity.aiot.ldms.dto.LdmsEmcallGroupInfo
import com.pluxity.aiot.ldms.dto.LdmsEmcallGroupResponse
import com.pluxity.aiot.ldms.dto.LdmsEmcallGroupStatus
import com.pluxity.aiot.ldms.dto.toResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

@Service
@ConditionalOnProperty("ldms.enabled", havingValue = "true")
class LdmsService(
    private val ldmsClient: LdmsClient,
    private val objectMapper: ObjectMapper,
) {
    fun findDisplays(): List<LdmsDisplayResponse> = ldmsClient.getEbrdList().map { it.toResponse() }

    fun findEmcallGroups(): List<LdmsEmcallGroupResponse> = ldmsClient.getEmcallGroupList().map { it.toResponse(parseStatus(it)) }

    private fun parseStatus(group: LdmsEmcallGroupInfo): LdmsEmcallGroupStatus? {
        val json = group.emcallGrpStatJson?.takeIf { it.isNotBlank() } ?: return null
        return runCatching { objectMapper.readValue(json, LdmsEmcallGroupStatus::class.java) }
            .onFailure { log.warn { "LDMS 송출그룹 상태 파싱 실패 (seq=${group.emcallGrpSeq}): $json" } }
            .getOrNull()
    }
}
