package com.pluxity.aiot.alarm.type

enum class SensorType(
    val objectId: String,
    val description: String,
) {
    RIVER_MONITOR("34950", "하천모니터링복합센서"),
    FLOOD("34952", "침수감지"),
    EDGE_DEVICE("34953", "엣지 디바이스 이벤트"),
    TEMPERATURE_HUMIDITY("34954", "온습도계"),
    SLOPE("34955", "경사"),
    FIRE("34956", "화재 감지"),
    DISPLACEMENT_GAUGE("34957", "DISPLACEMENT_GAUGE"),
    SUMMARY2("34958", "Summary2"),
    VIBRATION("34959", "진동계"),
    FORCE_PROTOCOL("34960", "Force Protocol"),
    FORCE_TEST_MODE("34961", "Force Test Mode"),
    TX_BUFFER_MODE("34962", "TxBufferMode"),
    CCTV("34963", "CCTV"),
    SPEAKER("34964", "스피커"),
    ;

    companion object {
        fun fromObjectId(objectId: String) =
            entries.find { it.objectId == objectId }
                ?: throw IllegalArgumentException("Unknown objectId: $objectId")
    }
}
