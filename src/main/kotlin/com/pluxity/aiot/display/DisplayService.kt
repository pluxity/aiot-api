package com.pluxity.aiot.display

import com.pluxity.aiot.broadcast.BroadcastStubSamples
import com.pluxity.aiot.broadcast.DeviceStatus
import com.pluxity.aiot.display.dto.DisplayResponse
import org.springframework.stereotype.Service

/**
 * TODO 스펙 공유용 스텁. 엔티티/리포지토리 연동은 후속 구현에서 채운다.
 */
@Service
class DisplayService {
    fun findAll(siteId: Long?): List<DisplayResponse> = SAMPLES.filter { siteId == null || it.site?.id == siteId }

    companion object {
        val SAMPLES =
            listOf(
                DisplayResponse(
                    id = 1L,
                    name = "정문 전광판",
                    deviceId = "LED-0001",
                    location = "정문 매표소 상단",
                    status = DeviceStatus.NORMAL,
                    site = BroadcastStubSamples.site,
                ),
                DisplayResponse(
                    id = 2L,
                    name = "산책로 전광판",
                    deviceId = "LED-0002",
                    location = "산책로 3구간",
                    status = DeviceStatus.OFFLINE,
                    site = BroadcastStubSamples.site,
                ),
            )
    }
}
