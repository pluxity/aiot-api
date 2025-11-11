package com.pluxity.aiot.sensor.type

import com.pluxity.aiot.data.dto.MetricDefinition
import com.pluxity.aiot.sensor.type.dto.DeviceProfileResponse

enum class SensorType(
    val id: Long,
    val objectId: String,
    val description: String,
    val version: String,
    val measureName: String = "",
    val abbreviation: AbbreviationData,
    val deviceProfiles: List<DeviceProfileEnum>,
) {
//    RIVER_MONITOR("34950", "하천모니터링복합센서"),
//    FLOOD("34952", "침수감지"),
//    EDGE_DEVICE("34953", "엣지 디바이스 이벤트"),
    TEMPERATURE_HUMIDITY(
        1,
        "34954",
        "온습도계",
        "1.0",
        "temperature_humidity",
        AbbreviationData("thm", "온습도계"),
        listOf(DeviceProfileEnum.TEMPERATURE, DeviceProfileEnum.HUMIDITY, DeviceProfileEnum.DISCOMFORT_INDEX),
    ),

//    SLOPE("34955", "경사"),
    FIRE(
        2,
        "34956",
        "화재 감지",
        "1.0",
        "fire_alarm",
        AbbreviationData("fir", "화재감지기"),
        listOf(DeviceProfileEnum.FIRE_ALARM),
    ),
    DISPLACEMENT_GAUGE(
        3,
        "34957",
        "DISPLACEMENT_GAUGE",
        "1.0",
        "displacement_gauge",
        AbbreviationData("tst", "변위계"),
        listOf(DeviceProfileEnum.ANGLE_X, DeviceProfileEnum.ANGLE_Y),
    ),
//    SUMMARY2("34958", "Summary2"),
//    VIBRATION("34959", "진동계"),
//    FORCE_PROTOCOL("34960", "Force Protocol"),
//    FORCE_TEST_MODE("34961", "Force Test Mode"),
//    TX_BUFFER_MODE("34962", "TxBufferMode"),
//    CCTV("34963", "CCTV"),
//    SPEAKER("34964", "스피커"),
    ;

    companion object {
        fun fromObjectId(objectId: String) =
            entries.find { it.objectId == objectId }
                ?: throw IllegalArgumentException("Unknown objectId: $objectId")
    }
}

// DeviceProfile 상수 정의
enum class DeviceProfileEnum(
    val id: Long,
    val description: String,
    val fieldKey: String,
    val fieldType: FieldType,
    val unit: String,
) {
    TEMPERATURE(1, "온도", "Temperature", FieldType.Float, "°C"),
    HUMIDITY(2, "습도", "Humidity", FieldType.Float, "%"),
    FIRE_ALARM(3, "화재감지", "Fire Alarm", FieldType.Boolean, ""),
    DISCOMFORT_INDEX(4, "불쾌지수", "DiscomfortIndex", FieldType.Float, ""),
    ANGLE_X(5, "X축 각도", "Angle-X", FieldType.Float, "°"),
    ANGLE_Y(6, "Y축 각도", "Angle-Y", FieldType.Float, "°"),
    ;

    fun toMetricDefinition() = MetricDefinition(fieldKey, unit)

    fun toResponse() =
        DeviceProfileResponse(
            id = this.id,
            fieldKey = this.fieldKey,
            description = this.description,
            fieldUnit = this.unit,
            fieldType = this.fieldType,
        )

    companion object {
        private val map = entries.associateBy(DeviceProfileEnum::id)

        fun findById(id: Long) = map[id] ?: throw IllegalArgumentException("Unknown id: $id")

        fun getDescriptionByFieldKey(fieldKey: String) = entries.find { it.fieldKey == fieldKey }?.description
    }
}

data class AbbreviationData(
    var abbreviationKey: String,
    var fullName: String,
)

enum class FieldType {
    String,
    Integer,
    Float,
    Boolean,
}
