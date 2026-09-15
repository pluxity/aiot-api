package com.pluxity.aiot.data

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
     * 경로는 "Mobius/{AE}/{deviceId}/{objectInstance}" 형식이다.
     * 디바이스 하나에 Object Instance가 여럿 달릴 수 있어 deviceId 약어(WFL, BOS…)와 맞는 Object를 우선하고,
     * 없으면 아는 Object 중 첫 것을 쓴다.
     */
    fun resolve(uril: List<String>): List<MobiusDevice> =
        uril
            .map { it.trimStart('/').split('/') }
            .filter { it.size == 4 }
            .groupBy({ it[2] }, { it[3] })
            .filterKeys { deviceId -> excludedDeviceMarkers.none { deviceId.contains(it) } }
            .mapNotNull { (deviceId, objectInstances) ->
                val candidates = objectInstances.filter { it.take(5) in knownObjectIds }.sorted()
                val preferred = preferredObjectId(deviceId)
                val chosen = candidates.firstOrNull { it.take(5) == preferred } ?: candidates.firstOrNull()
                chosen?.let { MobiusDevice(deviceId = deviceId, objectId = it) }
            }

    private fun preferredObjectId(deviceId: String): String? =
        deviceId
            .split('-', '_', ' ')
            .firstNotNullOfOrNull { objectIdByAbbreviation[it.lowercase()] }
}
