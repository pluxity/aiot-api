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
    TEMPERATURE_HUMIDITY(
        1,
        "34954",
        "온습도계",
        "1.0",
        "temperature_humidity",
        AbbreviationData("thm", "온습도계"),
        listOf(DeviceProfileEnum.TEMPERATURE, DeviceProfileEnum.HUMIDITY, DeviceProfileEnum.DISCOMFORT_INDEX),
    ),
    FIRE(
        2,
        "34956",
        "화재감지기",
        "1.0",
        "fire_alarm",
        AbbreviationData("fir", "화재감지기"),
        listOf(DeviceProfileEnum.FIRE_ALARM),
    ),
    DISPLACEMENT_GAUGE(
        3,
        "34955",
        "변위계",
        "1.0",
        "displacement_gauge",
        AbbreviationData("tst", "변위계"),
        listOf(DeviceProfileEnum.ANGLE_X, DeviceProfileEnum.ANGLE_Y),
    ),
    WASTE_FILL_LEVEL(
        4,
        "34957",
        "쓰레기 적재 감지기",
        "1.0",
        "waste_fill_level",
        AbbreviationData("wfl", "쓰레기 적재 감지기"),
        listOf(
            DeviceProfileEnum.CONTAINER_MODULE_ID,
            DeviceProfileEnum.ACTUAL_FILLING,
            DeviceProfileEnum.HIGH_THRESHOLD,
        ),
    ),
    FOREST_FIRE(
        5,
        "34958",
        "산불 감지기",
        "1.0",
        "forest_fire_detection",
        AbbreviationData("ffa", "산불 감지기"),
        listOf(
            DeviceProfileEnum.FOREST_FIRE_DETECTION,
            DeviceProfileEnum.TEMPERATURE,
            DeviceProfileEnum.HUMIDITY,
            DeviceProfileEnum.CO2,
            DeviceProfileEnum.CO,
            DeviceProfileEnum.TVOC,
            DeviceProfileEnum.FIRE_CAUSE_MASK,
        ),
    ),
    ODOR_MONITOR(
        6,
        "34959",
        "화장실 악취 감지기",
        "1.0",
        "odor_monitor",
        AbbreviationData("bos", "화장실 악취 감지기"),
        listOf(
            DeviceProfileEnum.TEMPERATURE,
            DeviceProfileEnum.HUMIDITY,
            DeviceProfileEnum.NH3,
            DeviceProfileEnum.H2S,
        ),
    ),
    PEOPLE_COUNTER(
        7,
        "34964",
        "피플카운터",
        "1.0",
        "people_counter",
        AbbreviationData("apc", "피플카운터"),
        listOf(DeviceProfileEnum.NUMBER_OF_VISITORS, DeviceProfileEnum.NUMBER_OF_LEAVERS),
    ),
    COMPOSITE_AIR_QUALITY(
        8,
        "34970",
        "복합 대기질 센서",
        "1.0",
        "composite_air_quality",
        AbbreviationData("caq", "복합 대기질 센서"),
        listOf(
            DeviceProfileEnum.TEMPERATURE,
            DeviceProfileEnum.HUMIDITY,
            DeviceProfileEnum.PM2_5,
            DeviceProfileEnum.PM10,
            DeviceProfileEnum.WIND_SPEED,
            DeviceProfileEnum.WIND_DIRECTION,
            DeviceProfileEnum.UVI,
            DeviceProfileEnum.LED_LIGHT,
        ),
    ),
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
    CONTAINER_MODULE_ID(7, "쓰레기통 모듈 번호", "ContainerModuleId", FieldType.Integer, ""),
    ACTUAL_FILLING(8, "감지된 적재높이", "ActualFilling", FieldType.Integer, "cm"),
    FOREST_FIRE_DETECTION(9, "산불 감지", "FireDetection", FieldType.Boolean, ""),
    CO2(10, "이산화탄소", "CO2", FieldType.Integer, "ppm"),
    CO(11, "일산화탄소", "CO", FieldType.Integer, "ppm"),
    TVOC(12, "휘발성유기화합물", "TVOC", FieldType.Integer, "ppb"),
    FIRE_CAUSE_MASK(13, "산불 원인", "FireCauseMask", FieldType.Integer, ""),
    NH3(14, "암모니아", "NH3", FieldType.Integer, "ppm"),
    H2S(15, "황화수소", "H2S", FieldType.Integer, "ppm"),
    NUMBER_OF_VISITORS(16, "입장객수", "NumberOfVisitors", FieldType.Integer, "People"),
    NUMBER_OF_LEAVERS(17, "퇴장객수", "NumberOfLeavers", FieldType.Integer, "People"),
    PM2_5(18, "초미세먼지", "PM2.5", FieldType.Integer, "㎍/㎥"),
    PM10(19, "미세먼지", "PM10", FieldType.Integer, "㎍/㎥"),
    WIND_SPEED(20, "풍속", "WindSpeed", FieldType.Integer, "m/s"),
    WIND_DIRECTION(21, "풍향", "WindDirection", FieldType.Integer, "°"),
    UVI(22, "자외선 지수", "UVI", FieldType.Integer, "Index"),
    LED_LIGHT(23, "LED 램프", "LED Light", FieldType.Integer, ""),
    HIGH_THRESHOLD(24, "만재 여부 판단 기준값", "HighThreshold", FieldType.Integer, "cm"),
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
