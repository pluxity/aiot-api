package com.pluxity.aiot.broadcast

import io.swagger.v3.oas.annotations.media.Schema

@Schema(
    description = "장치 상태 (NORMAL: 정상, OFFLINE: 오프라인, UNKNOWN: 업체 API 미연동으로 확인 불가)",
)
enum class DeviceStatus {
    NORMAL,
    OFFLINE,
    UNKNOWN,
}
