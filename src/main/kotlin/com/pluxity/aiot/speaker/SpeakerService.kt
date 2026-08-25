package com.pluxity.aiot.speaker

import com.pluxity.aiot.speaker.dto.SpeakerResponse
import org.springframework.stereotype.Service

/**
 * TODO 계약 확정용 스텁. 엔티티/리포지토리 연동은 후속 구현에서 채운다.
 */
@Service
class SpeakerService {
    fun findAll(siteId: Long?): List<SpeakerResponse> = emptyList()
}
