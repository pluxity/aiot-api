package com.pluxity.aiot.system.entity

import com.pluxity.aiot.feature.entity.Feature
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import java.util.function.Consumer
import java.util.stream.Collectors.toMap

@Entity
class DeviceType(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @Column(name = "object_id", nullable = false, unique = true)
    var objectId: String? = null,
    @Column(name = "description", nullable = false)
    var description: String? = null,
    @Column(name = "version", nullable = false)
    var version: String? = null,
) {
    @OneToMany(mappedBy = "deviceType", cascade = [CascadeType.ALL], orphanRemoval = true)
    var deviceProfileTypes: MutableSet<DeviceProfileType> = mutableSetOf()

    @OneToMany(mappedBy = "deviceType", cascade = [CascadeType.ALL], orphanRemoval = true)
    var deviceEvents: MutableSet<DeviceEvent> = mutableSetOf()

    @OneToMany(mappedBy = "deviceType", cascade = [CascadeType.ALL])
    var features: MutableSet<Feature> = mutableSetOf()

    fun update(
        objectId: String?,
        description: String?,
        version: String?,
    ) {
        this.objectId = objectId
        this.description = description
        this.version = version
    }

    fun removeDeviceProfile(deviceProfile: DeviceProfile) {
        deviceProfileTypes.removeIf { it.deviceProfile == deviceProfile }
    }

    fun getDeviceProfiles(): List<DeviceProfile> =
        deviceProfileTypes
            .mapNotNull { it.deviceProfile }

    fun addDeviceEvent(event: DeviceEvent) {
        deviceEvents.add(event)
        event.updateDeviceType(this)
    }

    fun updateDeviceEvents(events: List<DeviceEvent>) {
        // 현재 이벤트들을 맵으로 변환 (ID를 키로 사용)
        val currentEvents: MutableMap<Long, DeviceEvent> =
            deviceEvents
                .mapNotNull { event -> event.id?.let { it to event } }
                .toMap()
                .toMutableMap()

        // 새로운 이벤트 리스트를 순회하면서 처리
        events.forEach { newEvent ->
            val eventId = newEvent.id
            if (eventId != null) {
                // 기존 이벤트 업데이트
                currentEvents[eventId]?.let { existingEvent ->
                    existingEvent.update(newEvent.name, newEvent.imageUrl, newEvent.deviceLevel!!)
                    currentEvents.remove(eventId)
                }
            } else {
                // 새로운 이벤트 추가
                addDeviceEvent(newEvent)
            }
        }
        // 남은 이벤트들은 제거 (더 이상 사용되지 않는 이벤트)
        currentEvents.values.forEach(Consumer { o: DeviceEvent? -> deviceEvents.remove(o) })
    }

    fun addPoi(poi: Feature) {
        this.features.add(poi)
        if (poi.deviceType !== this) {
            poi.changeDeviceType(this)
        }
    }

    fun removePoi(poi: Feature) {
        this.features.remove(poi)
        if (poi.deviceType === this) {
            poi.changeDeviceType(null)
        }
    }
}
