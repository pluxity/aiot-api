package com.pluxity.aiot.data

import com.pluxity.aiot.data.dto.MobiusContainer
import com.pluxity.aiot.sensor.type.SensorType

data class MobiusDevice(
    val deviceId: String,
    val objectId: String,
)

object MobiusDeviceResolver {
    private val objectIdByAbbreviation = SensorType.entries.associate { it.abbreviation.abbreviationKey.lowercase() to it.objectId }
    private val knownObjectIds = SensorType.entries.map { it.objectId }.toSet()

    /** 테스트 장비와 온습도계(THM)는 동기화 대상에서 뺀다 */
    private val excludedDeviceMarkers = listOf("P-TST", "P-THM")

    /**
     * 디바이스 하나에 Object Instance가 여럿 달릴 수 있다(온습도계에 FillingLevel까지 붙어 있음).
     * deviceId 약어(THM, WFL…)와 맞는 Object를 우선하고, 없으면 아는 Object 중 첫 것을 쓴다.
     */
    fun resolve(
        devices: List<MobiusContainer>,
        objects: List<MobiusContainer>,
    ): List<MobiusDevice> {
        val objectsByParent = objects.groupBy { it.pi }
        return devices
            .filterNot { device -> excludedDeviceMarkers.any { device.rn.contains(it) } }
            .mapNotNull { device ->
                val candidates =
                    objectsByParent[device.ri]
                        .orEmpty()
                        .map { it.rn }
                        .filter { it.take(5) in knownObjectIds }
                        .sorted()
                val preferred = preferredObjectId(device.rn)
                val chosen = candidates.firstOrNull { it.take(5) == preferred } ?: candidates.firstOrNull()
                chosen?.let { MobiusDevice(deviceId = device.rn, objectId = it) }
            }
    }

    private fun preferredObjectId(deviceId: String): String? =
        deviceId
            .split('-', '_', ' ')
            .firstNotNullOfOrNull { objectIdByAbbreviation[it.lowercase()] }
}
