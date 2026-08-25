package com.pluxity.aiot.display

import com.pluxity.aiot.display.dto.DisplayResponse
import org.springframework.stereotype.Service

/**
 * TODO 계약 확정용 스텁. 엔티티/리포지토리 연동은 후속 구현에서 채운다.
 */
@Service
class DisplayService {
    fun findAll(siteId: Long?): List<DisplayResponse> = emptyList()
}
