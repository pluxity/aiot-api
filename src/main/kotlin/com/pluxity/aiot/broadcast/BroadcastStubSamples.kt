package com.pluxity.aiot.broadcast

import com.pluxity.aiot.global.response.BaseResponse
import com.pluxity.aiot.site.dto.SiteResponse

/**
 * TODO 스펙 공유용 임시 샘플 데이터.
 *  엔티티/업체 API 연동 구현 시 이 파일과 각 Service 의 참조를 함께 제거한다.
 */
object BroadcastStubSamples {
    val baseResponse =
        BaseResponse(
            createdAt = "2026-08-25T09:00:00",
            createdBy = "admin",
            updatedAt = "2026-08-25T09:00:00",
            updatedBy = "admin",
        )

    val site =
        SiteResponse(
            id = 1L,
            name = "중앙공원",
            description = "샘플 현장",
            location = "POLYGON((127.0 37.0, 127.1 37.0, 127.1 37.1, 127.0 37.1, 127.0 37.0))",
            baseResponse = baseResponse,
            thumbnail = null,
        )
}
